class Field {
    public String name;
    public int age;

    int timeFlies(int years) {
        age += years;
        System.out.println(name + "is " + years + " years older.");
        return age;
    }

}