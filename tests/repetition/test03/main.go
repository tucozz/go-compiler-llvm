func main() {
    values := []int{2, 4, 6, 8, 10};
    count := 0;
    flag := false;
    for _, value := range values {
        if value > 6 && count == 2 {
            flag = true;
            break;
        }
        if value == 4 {
            count = count + 1;
            continue;
        }
        count = count + 1;
    }
}