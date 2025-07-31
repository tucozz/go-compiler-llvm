i, j := 0, 10
f := func() int { return 7 }
ch := make(chan int)
r, w, _ := os.Pipe() // os.Pipe() returns a connected pair of Files and an error, if any
_, y, _ := coord(p)  // coord() returns three values; only interested in y coordinate