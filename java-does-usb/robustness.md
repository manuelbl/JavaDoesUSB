# Concurrency and Robustness Review — Windows Implementation

Review of the library's concurrency behavior, focused on the Windows implementation.
Threads involved: user threads, the "USB async IO" completion thread (`WindowsAsyncTask`),
and the "USB device monitor" thread (window message loop in `WindowsUsbDeviceRegistry`).

Findings are ordered by severity.

---

## 1. Potential permanent deadlock: completion handlers are invoked while holding the `WindowsAsyncTask` monitor — **FIXED (Windows & Linux)**

> **Status:** Fixed on 2026-07-03. `WindowsAsyncTask.completeTransfer` now removes the transfer
> from the map, copies the results out of the OVERLAPPED, and recycles the OVERLAPPED under the
> lock, then invokes the completion handler after releasing it, wrapped in try/catch so a throwing
> handler cannot kill the async IO thread (partially addressing finding 3 as well). Verified with
> the full hardware test suite (59 tests, all passing). macOS was already correct.
>
> The Linux counterpart is fixed the same way: `reapURBs` now reaps and unlinks transfers under the
> task monitor, collecting them into a list, and invokes the handlers after releasing it (in a
> `finally`, so already-reaped transfers still complete if reaping throws), each wrapped in
> try/catch. `removeFromAsyncIOCompletion` is no longer method-synchronized: the epoll removal and
> the stale-URB sweep each take the lock briefly, and the ENODEV completions are invoked outside
> it. This is safe against a concurrent submit on the same device because `LinuxUsbDevice.close()`
> and `LinuxUsbDevice.submitTransfer` serialize on the device monitor. Compile-verified only (see
> finding 2's note on Linux verification).
>
> **Residual (pre-existing, Linux):** the ENODEV completions in `removeFromAsyncIOCompletion` run
> on the closing thread, which holds the *device* monitor (`LinuxUsbDevice.close()` is
> synchronized). A timed-out waiter holding a transfer monitor and calling `abortTransfers` (wants
> the device monitor) while `close()` invokes that transfer's handler (wants the transfer monitor)
> could still cycle. This inversion predates this fix and no longer involves the async IO thread,
> so it can only stall the closing thread and the waiter, not the whole library. Windows doesn't
> have it (close-triggered completions arrive via the IO thread, which holds no device monitor).

`WindowsAsyncTask.completeTransfer()` (`WindowsAsyncTask.java:157`) is `synchronized` and calls
`transfer.completion().completed(transfer)` inside the lock. This creates a lock-order inversion
with the user-thread paths:

- **User thread A** (sync transfer with timeout): holds the `transfer` monitor
  (`synchronized (transfer)` block in `WindowsUsbDevice.transferOut`, `WindowsUsbDevice.java:338`)
  and, when the timeout fires, calls `abortTransfers(...)` from inside `waitForTransfer`
  (`UsbDeviceImpl.java:385`) → **wants the device monitor** (`abortTransfers` is `synchronized`,
  `WindowsUsbDevice.java:482`).
- **User thread B**: holds the device monitor (`submitTransferOut` is `synchronized`,
  `WindowsUsbDevice.java:413`) and calls `asyncTask.prepareForSubmission(...)` →
  **wants the asyncTask monitor**.
- **Async IO thread**: holds the asyncTask monitor in `completeTransfer` and calls
  `onSyncTransferCompleted` → **wants A's transfer monitor** (`UsbDeviceImpl.java:477`).

If A's transfer completes right as its timeout expires (exactly the window in which timeouts race
with completions), the cycle A→device→B, B→asyncTask→IO thread, IO thread→transfer→A closes and is
permanent. Because `WindowsAsyncTask` is a process-wide singleton, a stuck IO thread freezes
completions for **all** devices, and every no-timeout transfer then blocks forever in
`waitNoTimeout`. The trigger is realistic: one thread doing timed transfers while another thread
submits on the same or another device.

**Fix:** in `completeTransfer`, do the map removal / result copying / OVERLAPPED recycling under
the lock, but invoke the completion handler *after* releasing it. (The macOS `MacosAsyncTask` has
the same structural pattern and is worth the same check.)

## 2. Failed submission leaks the transfer registration — the Windows counterpart of the macOS fix is missing — **FIXED (Windows & Linux)**

> **Status:** Fixed on 2026-07-03. `WindowsAsyncTask.submissionFailed(transfer)` now undoes the
> registration when a `WinUsb_*` call fails synchronously: it removes the map entry, returns the
> OVERLAPPED to the pool, and clears the transfer's reference (safe because Win32 posts no
> completion packet for a synchronous failure). Called from the error paths of all three submit
> methods, mirroring the macOS fix. Verified with the full hardware test suite (59 tests, all
> passing).
>
> The same leak was discovered and fixed in Linux: `LinuxAsyncTask.submitTransfer` called
> `linkToUrb` (registers the URB→transfer mapping and takes a URB from the pool) and then threw on
> `SUBMITURB` ioctl failure without cleanup. A private `submissionFailed(transfer)` (safe to call
> under the already-held task monitor) now unlinks the transfer and recycles the URB — safe
> because a URB rejected by the ioctl is never queued and will never be reaped. Compile-verified
> only; the hardware test suite was run on Windows and does not exercise the Linux code path.

Commit `60a47af` ("Deregister transfer on exception (macOS)") added
`asyncTask.submissionFailed(transfer)` when an async submission fails synchronously. Windows has
the identical bug, unfixed: in `submitControlTransfer`, `submitTransferOut`, and `submitTransferIn`
(`WindowsUsbDevice.java:404–408, 422–426, 440–444`), `asyncTask.prepareForSubmission(transfer)`
registers the OVERLAPPED→transfer mapping and takes an OVERLAPPED from the pool; if `WinUsb_*` then
fails with anything other than `ERROR_IO_PENDING`, the exception path leaves the entry in
`requestsByOverlapped` forever and the OVERLAPPED never returns to `availableOverlappedStructs`.

Synchronous failures are the *normal* case right after an unplug, so an application that keeps
retrying on a hot-unplugged device leaks an OVERLAPPED, a map entry, and the pinned transfer buffer
per attempt.

**Fix:** add the same `submissionFailed()` cleanup to `WindowsAsyncTask` and call it from the three
submit methods' error paths.

## 3. The singleton async IO thread can die, silently hanging the whole library — **FIXED (Windows & Linux)**

> **Status:** Fixed on 2026-07-03 (handler-exception protection was already added with finding 1;
> macOS needed nothing — its upcall is fully wrapped and a run loop exit is logged).
>
> **Windows:** the completion loop body is wrapped in try/catch. Any exception — including a
> `GetQueuedCompletionStatus` failure with a null OVERLAPPED, whose error text is also fixed —
> now logs an ERROR, fails every transfer in `requestsByOverlapped` with
> `ERROR_OPERATION_ABORTED` (handlers invoked outside the lock, individually guarded), marks the
> task terminated, and exits. `prepareForSubmission` rejects new submissions with a clear
> `UsbException` once terminated, so callers fail fast instead of hanging in `waitNoTimeout`.
> The silent `return` on a successful dequeue with null OVERLAPPED ("registry closing?") was
> speculative — nothing posts such packets — and now goes through the same fatal path.
>
> **Linux:** same structure — the epoll loop body is wrapped; a non-EINTR `epoll_wait` failure
> (or an escaping exception) fails all pending transfers with `ECANCELED`, marks the task
> terminated, and exits; `submitTransfer` then rejects new work. In addition, two per-device reap
> failures no longer kill the thread: `EBADF` is treated as a benign race (the fd was closed
> concurrently — reachable from an ordinary `close()` while the event thread already holds a
> ready event) and any other unexpected reap errno degrades only that device (log, deregister
> from epoll to avoid a hot loop, continue serving other devices).
> `EPoll.removeFileDescriptor` now tolerates `EBADF` like `ENOENT`. `EBADF`/`ECANCELED` were
> added to the committed jextract errno bindings by hand (asm-generic values, valid on
> x64/ARM64) and to `gen_linux.sh` for the next regeneration on a Linux machine.
>
> Verified with the full hardware test suite on Windows (59 tests, all passing); the fatal paths
> themselves and the Linux changes are verified by inspection/compilation only.

Two ways `asyncCompletionTask()` (`WindowsAsyncTask.java:68`) can terminate:

- `GetQueuedCompletionStatus` fails with a null OVERLAPPED → `throwLastError` (line 86) kills the
  thread.
- Any `RuntimeException` escaping a completion handler — handlers run inline in this thread (see
  finding 1), so a bug or unexpected state in stream/user completion code is fatal to the loop.

There is no restart, and the failure mode is nasty: subsequent transfers submit fine but never
complete, so callers block forever in `waitNoTimeout` (unbounded) and
`flush()`/`waitForAvailableTransfer` (also unbounded by design).

**Fix:** wrap the loop body in a catch-log-continue (a failed handler must not stop dispatching
other devices' completions) and reserve thread death for truly unrecoverable port errors — ideally
then failing all pending transfers in `requestsByOverlapped` so waiters wake up.

Minor: the error text at line 86 says "SetupDiGetDeviceInterfaceDetailW" — a copy-paste from
elsewhere.

## 4. `claimInterfaceSynchronized` leaves inconsistent state on error paths

In `WindowsUsbDevice.java:215–217`, `firstIntfHandle.deviceHandle` and `winusbHandle` are assigned
**before** `asyncTask.addDevice(deviceHandle)`. If `addDevice` throws (CreateIoCompletionPort
failure), the catch closes the device handle but leaves both fields set — a later claim sees
`deviceHandle != null`, skips reopening, and submits I/O on a closed handle.

**Fix:** assign the fields only after `addDevice` succeeds (or null them in the catch, and
`WinUsb_Free` the interface handle too).

Related: when claiming an associated interface (e.g. interface 1 of a function starting at 0) opens
the device and then `WinUsb_GetAssociatedInterface` fails (line 228), the exception propagates with
the device left open but `deviceOpenCount == 0` and no interface claimed. Since `close()` only
releases *claimed* interfaces, that device handle (still registered with the completion port) leaks
until process exit.

## 5. Visibility: shared flags are neither volatile nor consistently synchronized

- `showAsOpen`: written under the device monitor, but `isOpened()` (`WindowsUsbDevice.java:114`)
  reads it unlocked — `checkIsOpen`/`checkIsClosed` can act on stale state on a different thread.
- `UsbDeviceImpl.connected`: written by the monitor thread in `disconnect()`, read unlocked via
  `isConnected()` and `checkIsClosed`.
- `UsbDeviceRegistry.onDeviceConnectedHandler` / `onDeviceDisconnectedHandler`: set by the app
  thread, read by the monitor thread with no happens-before edge — a handler registered after
  `start()` may never be seen.

All are cheap to fix with `volatile`. Practical impact is low (stale reads, not corruption), but
`isConnected()` returning `true` long after unplug is user-visible.

## 6. Minor observations

- **Data race on result fields in the timeout path**: `completeTransfer` writes
  `resultCode`/`resultSize` before taking the transfer monitor, while the timed-out waiter reads
  `transfer.resultCode()` holding it (`UsbDeviceImpl.java:384`). Consequences are benign (a
  redundant `WinUsb_AbortPipe`, or a timeout exception despite late data — inherent to the race
  anyway), but it's formally a data race.
- **User callbacks run on the monitor thread inside the window procedure**
  (`WindowsUsbDeviceRegistry.java:326–347`). A slow `onDeviceConnected`/`onDeviceDisconnected`
  handler stalls all further device notifications (and the `disconnect()` cleanup of other
  devices). Exceptions are caught, which is good; the latency constraint is worth documenting if it
  isn't already.
- `completeTransfer` dispatches all devices through one lock and one thread, so one slow handler
  delays every device's completions — currently fine since internal handlers are cheap queue
  operations, but it compounds finding 1's argument for invoking handlers outside the lock.

## What holds up well

The overall unplug/teardown story is sound: closing the WinUSB/file handles cancels pending I/O,
cancelled I/O still posts completion packets to the port, so blocked waiters wake with an error
result; the stream teardown paths (`close()`, `collectOutstandingTransfers`, abort-completion wait)
are all deadline-bounded so a genuinely lost completion degrades to a logged warning rather than a
hang — only the *non*-teardown unbounded waits remain exposed, and only via finding 3.

The `claimInterface` retry loop sleeps outside the device monitor, interrupts are deferred rather
than swallowed, `transfer.wait()` correctly releases the transfer monitor while submissions on
other threads proceed, and the copy-on-write device list with the case-insensitive Windows override
applied to both add and remove paths is correct. The timed-out-transfer buffers using
`Arena.ofAuto()` so an abandoned transfer can't have its buffer freed under a late completion is a
well-handled detail.

## Recommended priority

Findings 1, 2 and 3 are fixed on all platforms (see above). Next up: finding 4
(`claimInterfaceSynchronized` error-path state), then the `volatile` flags of finding 5.
The Linux fixes for findings 1–3 still need a hardware test run on a Linux machine.
