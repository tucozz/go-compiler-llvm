func test(x int, x string) {
    // Erro: parâmetro x redeclarado
}

func main() {
    var x int
    // Isso deveria ser OK - parâmetro vs variável local
}
