package main

func main() {
    sum := 0;
    for i := 0; i < 5; i = i + 1 {
        sum = sum + i;
        if sum == 6 {
            break;
        }
    }
    result := sum < 10;
}