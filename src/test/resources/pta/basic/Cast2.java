class Cast2{
    public static void main(String[] args) {
        m();
    }

    static void m() {
        Animal animal = new Animal();
        animal = new Dog();
        animal = new Cat();
        animal = new Pupi(); //may-fail

        Dog dog = (Dog) animal;

        Dog dog2 = new Dog();
        Pupi pupi = (Pupi) dog2; //fail

        Animal animal2 = new Dog();
        Dog dog4 = (Dog) animal2; //safe

        Dog dog5 = new Pupi();
        Animal animal3 = dog5;
        Pupi pupi1 = (Pupi) animal3; //safe

        Animal animal4 = new Cat();
        Dog dog6 = (Dog) animal4; //fail

        Animal animal5 = new Animal();
        Dog dog7 = (Dog) animal5; //fail
    }

}

class Animal{

}

class Dog extends Animal{

}

class Cat extends Animal{

}

class Pupi extends Dog{

}
