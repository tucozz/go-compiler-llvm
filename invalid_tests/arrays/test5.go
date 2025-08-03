func main() {
    var numbers []int
    var text string = "hello"
    var index string = "invalid"
    
    // Erro: usar string como índice de array
    var wrong int = numbers[index]
    
    // Erro: tentar acessar array em variável que não é array
    var wrong2 string = text[0]
    
    // Erro: array não declarado
    var wrong3 int = undeclaredArray[0]
}
