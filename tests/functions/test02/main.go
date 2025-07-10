func multiply(x int, y int, z int) int {
    return x * y * z;
}

func concatenate(first string, second string) string {
    return first + second;
}

func main() {
    var product int = multiply(2, 3, 4);
    var text string = concatenate("Hello", "World");
    var value int = 42;
}
