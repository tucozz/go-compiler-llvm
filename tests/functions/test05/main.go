func add(a int, b int) int {
    return a + b;
}

func noParams() {
    var x int = 5;
}

func main() {
    var result1 int = add(10);
    var result2 int = add(5, 3, 7);
    noParams(42);
    var undeclared int = missingFunction();
}
