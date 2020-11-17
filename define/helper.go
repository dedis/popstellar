package define

/* helper function to find an elem. in a slice.  returns index of elem + bool in slice*/
func Find(slice []int, val int) (int, bool) {
	for i, item := range slice {
		if item == val {
			return i, true
		}
	}
	return -1, false
}
