func calculate(a int, b int) int {
    return a + b * 2;
}

func main() {
    var x int = 10;
    var y int = 5;
    var result1 int = calculate(x, y);
    var result2 int = calculate(15, 25);
    var result3 int = calculate(x + 5, y - 2);
}
