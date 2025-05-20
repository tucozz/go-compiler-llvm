package main

func main() {
    numbers := []int{1, 3, 5, 7, 9};
    sum := 0;
    found := false;
    for i, value := range numbers {
        if value == 5 {
            found = true;
            break;
        }
        if i > 1 {
            continue;
        }
        sum = sum + value;
    }
}