func main() {
    var i int = 0
    var limit int = 10
    
    // For loop com break e continue válidos
    for i < limit {
        if i == 5 {
            i = i + 1
            continue
        }
        
        if i == 8 {
            break
        }
        
        i = i + 1
    }
}
