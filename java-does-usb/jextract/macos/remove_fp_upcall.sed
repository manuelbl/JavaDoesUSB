/The function pointer signature/,/        \}/ {
        s/^.*function pointer signature.*$/         *\//p
        d

}
/MethodHandle UP\$MH = /,/        \}/d
