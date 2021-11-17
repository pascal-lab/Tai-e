class NestedHeap {
    public static void main(String args[]) {
        House house1 = new House();
        House house2 = new House();
        Bed bed = house1.getBedRoom().bed;
    }
}

class House {
    BedRoom bedRoom;

    public House() {
        this.bedRoom = new BedRoom();
    }

    public BedRoom getBedRoom() {
        return this.bedRoom;
    }
}

class BedRoom {
    Bed bed;

    public BedRoom() {
        this.bed = new Bed();
    }

    public Bed getBed() {
        return this.bed;
    }
}

class Bed {

}