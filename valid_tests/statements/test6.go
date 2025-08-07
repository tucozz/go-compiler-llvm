func main() {
    var sum int
    sum = 0
    
    // For clássico sem condição (infinito com incremento): init; ; post
    for i := 0; ; i++ {
        sum = sum + i
        if i >= 3 {
            break
        }
    }
}
