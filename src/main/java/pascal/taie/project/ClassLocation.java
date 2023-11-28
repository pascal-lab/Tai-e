package pascal.taie.project;

class ClassLocation {

    private final String fullClassLocation;

    private int index;

    public ClassLocation(String fullClassLocation) {
        this.fullClassLocation = fullClassLocation;
        index = 0;
    }

    /**
     *
     * @return whether current location has next.
     * <br/>e.g. "pascal.taie.project.ClassLocation": true
     * <br/>     "ClassLocation": true
     * <br/>     "": false
     */
    public boolean hasNext() {
        return index < fullClassLocation.length();
    }

    /**
     *
     * @return current level of location.
     * <br/>e.g. "pascal.taie.project.ClassLocation": "pascal"
     * <br/>     "ClassLocation": "ClassLocation"
     */
    public String next() throws IndexOutOfBoundsException {
        if (index >= fullClassLocation.length()) throw new IndexOutOfBoundsException();
        int nextDot = fullClassLocation.indexOf('.', index);
        String result;
        if (nextDot != -1) {
            result = fullClassLocation.substring(index, nextDot);
            index = nextDot + 1;
        } else {
            result = fullClassLocation.substring(index);
            index = fullClassLocation.length();
        }
        return result;
    }

    /**
     * set the index to 0
     */
    public void clear() {
        this.index = 0;
    }
}
