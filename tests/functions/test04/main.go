func duplicateFunction() int {
    return 10;
}

func duplicateFunction() string {
    return "error";
}

func main() {
    var value int = duplicateFunction();
}
