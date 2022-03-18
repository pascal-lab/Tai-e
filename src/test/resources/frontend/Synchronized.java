class Synchronized {
    public void onlyMe(Object f) {
        synchronized(f) {
            int i = 1 + 1;
        }
    }
}