func main() {
	var a int
    var b float64
    var c bool
    var d string

	a = 5 / 5;
	a = 5 / 5.0;
	a = 5 / true;
	a = 5 / "Hello";
	b = 5.0 / 5;
	b = 5.0 / 5.0;
	b = 5.0 / true;
	b = 5.0 / "Hello";
	c = true / 5;
	c = true / 5.0;
	c = true / true;
	c = true / "Hello";
	d = "Hello" / 5;
	d = "Hello" / 5.0;
	d = "Hello" / true;
	d = "Hello" / "World";
}