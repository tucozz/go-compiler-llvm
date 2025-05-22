func main() {
    i := 0;
    flag := true;
    for flag == true && i < 5 {
        i = i + 1;
        if i == 5 {
            flag = false;
        }
    }
}