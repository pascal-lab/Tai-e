 /*
 * This testcase is taken from Rinard et al.
 */

public class LinkedList {
    static float sumX(List list){
        float s=0;
        Iterator it=list.iterator();
        while(it.hasNext()){
            Point p=(Point) it.next();
            s+=p.x;
        }
        return s;
    }

    static void flipAll(List list){
        Iterator it=list.iterator();
        while(it.hasNext()){
            Point p=(Point) it.next();
            p.flip();
        }
    }

    public static void main(String args[]){
        List list=new List();
        list.add(new Point(1,2));
        list.add(new Point(2,3));
        sumX(list);
        flipAll(list);
    }
}

class List {
    Cell head=null;
    void add(Object e){
        head=new Cell(e,head);
    }
    Iterator iterator(){
        return new ListItr(head);
    }
}

class Cell {
    Cell(Object d,Cell n){
        data=d;
        next=n;
    }
    Object data;
    Cell next;
}

interface Iterator {
    boolean hasNext();
    Object next();
}

class ListItr implements Iterator{
    ListItr(Cell head){
        cell=head;
    }
    Cell cell;
    public boolean hasNext(){
        return cell != null;
    }
    public Object next(){
        Object result = cell.data;
        cell=cell.next;
        return result;
    }
}

class Point {
    Point(float x,float y){
        this.x=x;
        this.y=y;
    }
    float x,y;
    void flip(){
        float t=x;x=y;y=t;
    }
}
