# Concurrency and Robustness Review

Merged from two parallel reviews: one focused on the **Windows** implementation (with the
resulting fixes ported to **Linux**), one focused on the **macOS** implementation (with several
fixes landing in the shared common classes, benefiting all three platforms).

Threads involved:

- user threads submitting transfers and blocking on results,
- the "USB async IO" completion thread — I/O completion port loop on Windows
  (`WindowsAsyncTask`), epoll loop on Linux (`LinuxAsyncTask`), CFRunLoop on macOS
  (`MacosAsyncTask`); a **process-wide singleton** on every platform,
- the "USB device monitor" thread — window message loop (`WindowsUsbDeviceRegistry`),
  udev monitoring (Linux), IOKit notification run loop (`MacosUsbDeviceRegistry`).

Files reviewed on the macOS side: `MacosUsbDevice`, `MacosAsyncTask`, `MacosUsbDeviceRegistry`,
`MacosEndpointInputStream` / `MacosEndpointOutputStream`, `MacosTransfer`, `IoKitHelper`, and the
shared base classes `EndpointInputStream`, `EndpointOutputStream`, `Transfer`, `UsbDeviceImpl`,
`UsbDeviceRegistry`.

Findings are ordered by severity. Fixed findings keep their status note followed by the original
description.

---

## 1. Potential permanent deadlock: completion handlers invoked while holding the `WindowsAsyncTask` monitor — **FIXED (Windows & Linux; macOS was already correct)**

> **Status:** Fixed on 2026-07-03. `WindowsAsyncTask.completeTransfer` now removes the transfer
> from the map, copies the results out of the OVERLAPPED, and recycles the OVERLAPPED under the
> lock, then invokes the completion handler after releasing it, wrapped in try/catch so a throwing
> handler cannot kill the async IO thread (partially addressing finding 3 as well). Verified with
> the full hardware test suite (59 tests, all passing).
>
> macOS was already correct: `completed()` is invoked outside the `asyncTask` monitor
> (`MacosAsyncTask.java:197-203`), avoiding the lock cycle with the submit path
> (`device → asyncTask → transfer`).
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
the lock, but invoke the completion handler *after* releasing it.

## 2. Failed submission leaks the transfer registration — **FIXED (macOS, Windows & Linux)**

Both reviews found the same bug independently, on different platforms. The macOS fix landed first
(commit `60a47af`, "Deregister transfer on exception (macOS)"); the Windows and Linux counterparts
followed.

> **Status (macOS):** Fixed. `MacosAsyncTask` now has a `submissionFailed` counterpart to
> `prepareForSubmission` that removes the transfer's map entry. Each of the three submit methods
> (`submitTransferIn`, `submitTransferOut`, `submitControlTransfer`) calls it in the native-error
> path, before `throwException`, so a failed submission no longer leaks an entry. Removal (rather
> than the alternative of registering only after a successful native call) was chosen deliberately:
> the completion callback runs on the async-IO run-loop thread and could otherwise race ahead of a
> post-success registration, find no map entry, and hang the waiting thread. Removing only on
> failure is race-free because a failed native submission produces no completion callback.
>
> **Status (Windows):** Fixed on 2026-07-03. `WindowsAsyncTask.submissionFailed(transfer)` now
> undoes the registration when a `WinUsb_*` call fails synchronously: it removes the map entry,
> returns the OVERLAPPED to the pool, and clears the transfer's reference (safe because Win32
> posts no completion packet for a synchronous failure). Called from the error paths of all three
> submit methods, mirroring the macOS fix. Verified with the full hardware test suite (59 tests,
> all passing).
>
> **Status (Linux):** the same leak was discovered and fixed: `LinuxAsyncTask.submitTransfer`
> called `linkToUrb` (registers the URB→transfer mapping and takes a URB from the pool) and then
> threw on `SUBMITURB` ioctl failure without cleanup. A private `submissionFailed(transfer)` (safe
> to call under the already-held task monitor) now unlinks the transfer and recycles the URB —
> safe because a URB rejected by the ioctl is never queued and will never be reaped.
> Compile-verified only; the hardware test suite was run on Windows and does not exercise the
> Linux code path.

The original descriptions follow.

**macOS:** `prepareForSubmission` (`MacosAsyncTask.java:179-184`) registers the transfer in
the map *before* the native `ReadPipeAsync` / `WritePipeAsync` / `DeviceRequestAsync` call. If
that native call returns an error (`MacosUsbDevice.java:522`, `552`, `571`), the method throws and
the map entry is never removed (no completion will fire for it). Each failed submission
permanently leaks one entry in the singleton's map. Not fatal, but unbounded over a long-running
process that hits repeated submit failures.

**Windows:** in `submitControlTransfer`, `submitTransferOut`, and `submitTransferIn`
(`WindowsUsbDevice.java:404–408, 422–426, 440–444`), `asyncTask.prepareForSubmission(transfer)`
registers the OVERLAPPED→transfer mapping and takes an OVERLAPPED from the pool; if `WinUsb_*` then
fails with anything other than `ERROR_IO_PENDING`, the exception path leaves the entry in
`requestsByOverlapped` forever and the OVERLAPPED never returns to `availableOverlappedStructs`.
Synchronous failures are the *normal* case right after an unplug, so an application that keeps
retrying on a hot-unplugged device leaks an OVERLAPPED, a map entry, and the pinned transfer buffer
per attempt.

## 3. The singleton async IO thread can die, silently hanging the whole library — **FIXED (all platforms)**

> **Status:** Fixed on 2026-07-03 (handler-exception protection was already added with finding 1).
>
> **macOS:** `MacosAsyncTask.asyncIOCompleted` now null-checks the map lookup (logging and
> ignoring a completion for an unknown transfer ID) and wraps the whole callback body in a
> `try/catch` that logs and swallows, so a bad completion can no longer escape into `CFRunLoopRun`
> and take down the shared IO thread. In addition, an unexpected `CFRunLoopRun` exit is now logged
> (WARNING) in both `MacosAsyncTask.asyncIOCompletionTask` and
> `MacosUsbDeviceRegistry.monitorDevices`, so a dead run loop is observable instead of silent.
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

The original descriptions follow.

**Windows:** two ways `asyncCompletionTask()` (`WindowsAsyncTask.java:68`) can terminate:

- `GetQueuedCompletionStatus` fails with a null OVERLAPPED → `throwLastError` (line 86) kills the
  thread.
- Any `RuntimeException` escaping a completion handler — handlers run inline in this thread (see
  finding 1), so a bug or unexpected state in stream/user completion code is fatal to the loop.

There is no restart, and the failure mode is nasty: subsequent transfers submit fine but never
complete, so callers block forever in `waitNoTimeout` (unbounded) and
`flush()`/`waitForAvailableTransfer` (also unbounded by design).

Minor: the error text at line 86 said "SetupDiGetDeviceInterfaceDetailW" — a copy-paste from
elsewhere (fixed along with the above).

**macOS:** `MacosAsyncTask.java:194-204`:

```java
transfer = transfersById.remove(refcon.address());
transfer.setResultCode(result);   // NPE if remove() returned null
```

There was no null check and no `try/catch` around the whole callback. If a completion ever fires
for an ID not in the map — a double completion (e.g. a transfer that both times out/aborts *and*
completes), a spurious callback, or a bug in ID bookkeeping — this throws inside an upcall from
`CFRunLoopRun`. Because `MacosAsyncTask` is a **process-wide singleton** servicing *every* device
and interface, losing that thread hangs all outstanding and future async transfers for the whole
library, not just one device.

Related (originally a separate low-severity note): `MacosUsbDeviceRegistry.monitorDevices` and
`MacosAsyncTask.asyncIOCompletionTask` both call `CFRunLoopRun()` and assumed it never returns. If
it ever does (e.g. all sources removed), the thread exits silently and device monitoring / async
IO stops with no signal to the application. Both sites now log a warning.

## 4. Teardown waits (`collectOutstandingTransfers` / output-stream drain) could hang forever — **FIXED (all platforms, common code)**

> **Status:** Fixed. The teardown waits are now bounded. `EndpointInputStream.collectOutstandingTransfers`
> and a dedicated bounded drain in `EndpointOutputStream.close()` poll with a shared 1 s
> deadline (`TEARDOWN_TIMEOUT_MS`) instead of blocking on `take()`. When the deadline expires
> with transfers still outstanding, the remaining transfers are abandoned and a warning is
> logged rather than freezing the application thread. Abandoning is safe because buffers come
> from an auto arena and survive a late completion writing into them. Interruption during the
> drain is deferred (no busy-spin) and re-asserted afterwards. The normal `read()`/`write()`
> data path and the public `flush()` keep their unbounded, correct blocking-stream semantics.
> The fix lives in the shared common base classes, so all three platforms benefit.

The original description (from the macOS review) follows.

`EndpointInputStream.java:215-223` and `EndpointOutputStream.java:194-213` both
block until *every* outstanding transfer reports back:

```java
while (numOutstandingTransfers > 0)
    waitForCompletedTransfer();     // completedTransferQueue.take()
```

The entire teardown correctness rests on the assumption that **every submitted
transfer eventually produces exactly one completion callback.** In the normal
path this holds (`AbortPipe` / `USBInterfaceClose` abort pending pipes and
deliver completions). But there are windows where it may not:

- In `close()` (`EndpointInputStream.java:112-124`), if `device.abortTransfers`
  throws (device already disconnected), the abort is silently swallowed and we
  proceed straight to `collectOutstandingTransfers()`. We then rely entirely on
  the monitor thread's `device.close()` having already aborted the same pipes.
  If that hasn't happened yet, or if a completion is lost, `take()` blocks with
  no timeout and no interruptibility — a permanent hang of the application
  thread.
- The ordering guarantee in `MacosUsbDevice.close()` (abort → then post
  source-removal message) only holds if `USBInterfaceClose` has *signaled* the
  abort completions to the run loop before it returns. If IOKit signals them
  slightly later, the source-removal message can be processed first and the
  completions dropped → hang.

## 5. Interruption caused a busy-spin, and transfers are effectively non-interruptible — **FIXED (all platforms, common code)**

> **Status:** Fixed. Every blocking wait now uses a deferred-interrupt pattern. A caught
> `InterruptedException` sets a local `wasInterrupted` flag instead of re-asserting
> `Thread.currentThread().interrupt()` inside the loop; the flag is re-asserted only
> once the wait returns. Because the interrupt flag is no longer set *during* the loop,
> the blocking primitive (`queue.take()` / `transfer.wait()` / `Thread.sleep()`) blocks
> normally on the next iteration instead of throwing immediately — eliminating the 100%
> CPU spin. Applied to all six sites: `EndpointInputStream.waitForCompletedTransfer`,
> `EndpointOutputStream.waitForAvailableTransfer`, `UsbDeviceImpl.waitNoTimeout` and
> `waitWithTimeout`, plus the retry-backoff sleeps in `MacosUsbDevice.open()` and
> `WindowsUsbDevice.claimInterface()`. As part of this, `waitWithTimeout` now recomputes
> its remaining timeout on both the normal and interrupted paths, so it stays bounded by
> the original expiration and its timed-out return value stays accurate.
>
> Transfers remain intentionally **non-interruptible** (interruption is deferred, not
> turned into cancellation): a blocked USB call still returns only when the transfer
> completes, but the interrupt is preserved and observed afterwards. True cancellation
> was rejected because the only abort primitive (`AbortPipe`) tears down the whole
> endpoint, and the two call sites have incompatible exception vocabularies (checked
> `IOException` on the streams vs. unchecked `UsbException` on control transfers).

The original description follows.

Every blocking wait swallowed `InterruptedException`, re-asserted the interrupt
flag, and looped:

- `waitForCompletedTransfer` — `EndpointInputStream.java:194-204`
- `waitForAvailableTransfer` — `EndpointOutputStream.java:224-242`
- `waitNoTimeout` / `waitWithTimeout` — `UsbDeviceImpl.java:390-418`

Once the interrupt flag is set, the next `queue.take()` / `transfer.wait()`
throws `InterruptedException` **immediately**, which is caught, re-sets the
flag, and loops again. If the awaited completion hasn't arrived yet, this spins
at 100% CPU until it does. For `waitNoTimeout` with an unplugged device that
never delivers a completion, it spins indefinitely. Beyond the CPU cost, the
practical effect is that a blocked USB call cannot be cancelled by interrupting
the thread — the interrupt is neither honored nor cleanly deferred.

## 6. `waitWithTimeout` post-timeout `waitNoTimeout` assumed a completion still comes — **FIXED (all platforms, common code)**

> **Status:** Fixed. The post-timeout drain is now bounded. After a timeout, `UsbDeviceImpl.waitForTransfer`
> aborts the transfer and then waits for the abort's completion via `waitWithTimeout` with a fixed
> `ABORT_COMPLETION_TIMEOUT_MS` (1 s) bound instead of the unbounded `waitNoTimeout`. If the abort's
> completion never arrives (device vanished such that neither the transfer nor the abort yields a
> callback), the transfer is abandoned and a warning is logged; the method still throws the same
> `UsbTimeoutException`, so the user-facing outcome is unchanged. The no-timeout path
> (`timeout <= 0`) intentionally keeps its unbounded `waitNoTimeout` — a caller that asked for no
> timeout has opted into blocking.
>
> Abandoning is only memory-safe because the buffers that can reach this path — those in the
> synchronous `transferIn` / `transferOut` on all three platforms — were switched from
> `Arena.ofConfined()` to `Arena.ofAuto()`, so a late completion writing into the buffer can never
> touch freed memory. Control-transfer buffers stay confined: they only ever use the no-timeout path
> and so cannot reach the drain.

The original description follows.

`UsbDeviceImpl.java:368-388`: on timeout, `abortTransfers` is called and then
`waitNoTimeout(transfer)` blocks for the abort completion. This is correct *if*
the abort produces a completion. If the device vanished such that neither the
timeout nor the abort yields a callback, `waitNoTimeout` hangs (same root cause
as finding 4). The timeout feature gives users an expectation of bounded blocking
that the post-timeout `waitNoTimeout` quietly breaks.

## 7. `claimInterfaceSynchronized` leaves inconsistent state on error paths (Windows) — **FIXED**

> **Status:** Fixed on 2026-07-06. Both error paths in `claimInterfaceSynchronized` are addressed:
>
> - The device-open block now commits `firstIntfHandle.deviceHandle` / `winusbHandle` only *after*
>   the last fallible operation (`asyncTask.addDevice`) has succeeded, so no error path can leave
>   the fields pointing at a closed handle. The catch block cleans up purely local state — and now
>   also frees the WinUSB interface handle (previously leaked) before closing the device handle,
>   mirroring the cleanup order of `releaseInterface`. Closing the handle is a complete undo:
>   `addDevice` can only fail if `CreateIoCompletionPort` itself failed, in which case the handle
>   was never associated with the completion port, so no stray completion packets can result.
> - The associated-interface path is wrapped so that a `WinUsb_GetAssociatedInterface` failure
>   undoes the device open if (and only if) this call performed it, tracked by a local
>   `deviceOpenedHere` flag: free the first interface handle, close the device handle, null both
>   fields, rethrow. If the device was already open on entry, nothing is undone — the handle is
>   legitimately in use by a previously claimed interface, and no state was mutated.
>
> Compile-verified only: the error paths themselves (`CreateIoCompletionPort` /
> `WinUsb_GetAssociatedInterface` failures) require injected native failures and are verified by
> inspection. A hardware regression run on Windows is pending (see note at the end).

The original description follows.

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

## 8. Visibility: shared flags are neither volatile nor consistently synchronized — **FIXED (all platforms)**

> **Status:** Fixed on 2026-07-06. All three listed items are now `volatile`:
> `WindowsUsbDevice.showAsOpen`, `UsbDeviceImpl.connected`, and the two registry handler fields.
> The same unlocked-`isOpened()` pattern exists on the other two platforms (the finding named
> Windows only because that's where the review looked) and was fixed there too: `LinuxUsbDevice.fd`
> and the `MacosUsbDevice.claimedInterfaces` reference are also `volatile` now (for the latter,
> `isOpened()` only null-checks the reference; the list contents are only accessed while holding
> the device monitor, so `volatile` on the reference suffices).
>
> In addition, `emitOnDeviceConnected` / `emitOnDeviceDisconnected` now read the handler field once
> into a local before the null check. With `volatile` alone, a concurrent
> `setOnDeviceConnected(null)` between the null check and the invocation would have thrown an NPE
> that the catch block then mislabels as an exception *inside* the user's handler.
>
> Compile-verified; pure visibility changes of this kind are not exercisable by the test suite.

The original description follows.

- `showAsOpen`: written under the device monitor, but `isOpened()` (`WindowsUsbDevice.java:114`)
  reads it unlocked — `checkIsOpen`/`checkIsClosed` can act on stale state on a different thread.
- `UsbDeviceImpl.connected`: written by the monitor thread in `disconnect()`, read unlocked via
  `isConnected()` and `checkIsClosed`.
- `UsbDeviceRegistry.onDeviceConnectedHandler` / `onDeviceDisconnectedHandler`: set by the app
  thread, read by the monitor thread with no happens-before edge — a handler registered after
  `start()` may never be seen.

All are cheap to fix with `volatile`. Practical impact is low (stale reads, not corruption), but
`isConnected()` returning `true` long after unplug is user-visible.

## 9. `messagePort` (remote) creation is unchecked (macOS) — **FIXED**

> **Status:** Fixed on 2026-07-06. `startAsyncIOThread` now null-checks all *three* CF creations
> that can return NULL — `CFMessagePortCreateLocal`, `CFMessagePortCreateRunLoopSource`, and
> `CFMessagePortCreateRemote` (a NULL anywhere in the chain causes the same downstream damage,
> e.g. when bootstrap-port registration fails in a sandboxed process) — and throws a `UsbException`
> naming the failed call. The remote port is assigned to the `messagePort` field only after the
> check, so `removeEventSource` can never send on a NULL port.
>
> The naive throw would have traded the leak for a hang: the singleton would be stuck in state
> `STARTING` and every later `addEventSource` call would block forever in `waitForRunLoopReady`.
> The catch block therefore releases the partially created ports (`CFRelease`; releasing the last
> reference also invalidates the local port and deregisters its name, so a retry can recreate it)
> and resets the state to `NOT_STARTED`, so a later `open()` retries cleanly. The exception
> surfaces from `MacosUsbDevice.open()` before anything is registered with the run loop.
>
> Verified with the full hardware test suite on macOS (59 tests, all passing); the failure paths
> themselves require injected CF failures and are verified by inspection.

The original description follows.

`MacosAsyncTask.java:135`: `CFMessagePortCreateRemote` can return NULL (e.g. a
race where the local port's run-loop source isn't serviced yet).
`removeEventSource` would then send on a NULL port and the event source would
never be removed — a slow leak of run-loop sources. Worth asserting non-null at
creation.

## 10. Magic-number reinterpret in `loadDescription` (macOS) — **FIXED**

> **Status:** Fixed on 2026-07-06. The pointer returned by `GetConfigurationDescriptorPtr` is now
> first reinterpreted to exactly `ConfigurationDescriptor.LAYOUT.byteSize()` (9 bytes) — the
> minimum needed to read the header — then, after a sanity check that `wTotalLength` is at least
> the header size (throwing a `UsbException` naming the bogus value otherwise), reinterpreted to
> exactly `totalLength`. No magic number remains, and the view never claims memory beyond the
> kernel buffer, which IOKit itself sized from the same `wTotalLength` field when caching the
> descriptor. Verified with the full hardware test suite on macOS (59 tests, all passing) —
> `loadDescription` runs during enumeration of every connected device.

The original description follows.

`MacosUsbDevice.java:194`: `dereference(descPtrHolder).reinterpret(999999)`
before re-slicing to `totalLength()`. It's bounded immediately afterward so it's
safe in practice, but the arbitrary size is fragile if a malformed descriptor
ever reports a `totalLength` beyond the real buffer.

---

## Open findings

## 11. `open()` holds the device monitor across up to 4×90 ms sleeps (macOS)

`MacosUsbDevice.java:103-135` is `synchronized` and sleeps while retrying
`USBDeviceOpenSeize`. Any other thread calling a synchronized method on that
device blocks for up to ~360 ms. Minor, and only during open, but worth noting.

## 12. Minor observations

- **Data race on result fields in the timeout path** (Windows): `completeTransfer` writes
  `resultCode`/`resultSize` before taking the transfer monitor, while the timed-out waiter reads
  `transfer.resultCode()` holding it (`UsbDeviceImpl.java:384`). Consequences are benign (a
  redundant `WinUsb_AbortPipe`, or a timeout exception despite late data — inherent to the race
  anyway), but it's formally a data race.
- **User callbacks run on the monitor thread inside the window procedure**
  (`WindowsUsbDeviceRegistry.java:326–347`). A slow `onDeviceConnected`/`onDeviceDisconnected`
  handler stalls all further device notifications (and the `disconnect()` cleanup of other
  devices). Exceptions are caught, which is good; the latency constraint is worth documenting if it
  isn't already. The same applies to the macOS and Linux monitor threads.
- `completeTransfer` dispatches all devices through one lock and one thread, so one slow handler
  delays every device's completions — currently fine since internal handlers are cheap queue
  operations, but it compounds finding 1's argument for invoking handlers outside the lock.

---

## What holds up well

### Windows / Linux

The overall unplug/teardown story is sound: closing the WinUSB/file handles cancels pending I/O,
cancelled I/O still posts completion packets to the port, so blocked waiters wake with an error
result; the stream teardown paths (`close()`, `collectOutstandingTransfers`, abort-completion wait)
are all deadline-bounded so a genuinely lost completion degrades to a logged warning rather than a
hang.

The `claimInterface` retry loop sleeps outside the device monitor, interrupts are deferred rather
than swallowed, `transfer.wait()` correctly releases the transfer monitor while submissions on
other threads proceed, and the copy-on-write device list with the case-insensitive Windows override
applied to both add and remove paths is correct. The timed-out-transfer buffers using
`Arena.ofAuto()` so an abandoned transfer can't have its buffer freed under a late completion is a
well-handled detail.

### macOS

- **`synchronized` on the device** serializes submission against `close()` /
  `disconnect()`, so native interface/device pointers can't be released out
  from under a submitting thread (as the class Javadoc promises).
- **The wait/notify guard is correct**: `asyncIOCompleted` sets `resultSize` /
  `resultCode` *before* calling the completion handler, and `waitNoTimeout`
  loops on `resultSize() == -1` under the `transfer` monitor. A completion that
  races ahead of `wait()` is not lost
  (`MacosUsbDevice.java:407-410`, `UsbDeviceImpl.java:391-400`).
- **`completed()` is invoked outside the `asyncTask` monitor**
  (`MacosAsyncTask.java:197-203`), avoiding a lock cycle with the submit path
  (`device → asyncTask → transfer`).
- **Deferred event-source removal via the message port**
  (`MacosAsyncTask.removeEventSource`) is a deliberate and correct move to keep
  completion callbacks ordered ahead of source removal on the run-loop thread.
- **Stream buffers use `Arena.ofAuto()`** (`EndpointInputStream.java:66`), so a
  completion callback that writes into a buffer *after* the stream/device is
  gone won't touch freed memory — it stays alive until GC. Good defensive
  choice given the async model.

### Concurrency model — verdict (macOS)

The core two-thread interaction (app thread submitting/waiting vs. the run-loop
IO thread completing) is **sound**: lock ordering is consistent, there's no
reverse-order acquisition, the notify guard is correct, and buffer lifetime is
handled via `ofAuto`. Disconnect-during-blocking-transfer works *as long as
IOKit delivers a completion for every aborted transfer* — and that single
assumption was the load-bearing one. Findings 3, 4, and 6 were all facets of
"what happens when a completion is late, duplicated, or missing," which is
precisely the unplug scenario; all three are now hardened (null-safe callback,
bounded teardown waits, and treating a missing completion as a recoverable
timeout rather than an infinite block).

---

## Recommended priority

Findings 1–10 are fixed (see above). Findings 11–12 are low-priority robustness notes.

The Linux fixes for findings 1–3 still need a hardware test run on a Linux machine, and the
finding 7 fix a hardware regression run on Windows.
