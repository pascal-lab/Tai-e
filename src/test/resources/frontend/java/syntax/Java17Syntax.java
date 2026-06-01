import java.util.ArrayList;
import java.util.List;

/**
 * Tests Java 12-17 syntax: switch expressions with arrow labels and yield,
 * text blocks, records with compact constructors, local records, pattern
 * matching for instanceof, sealed classes, and non-sealed classes.
 */
public class Java17Syntax {

    sealed interface Shape permits Circle, Rectangle, Polygon {
    }

    record Circle(int radius) implements Shape {

        Circle {
            if (radius < 0) {
                throw new IllegalArgumentException("negative radius");
            }
        }
    }

    record Rectangle(int width, int height) implements Shape {

        Rectangle {
            if (width < 0 || height < 0) {
                throw new IllegalArgumentException("negative size");
            }
        }

        int area() {
            return width * height;
        }
    }

    static non-sealed class Polygon implements Shape {

        private final int sides;

        Polygon(int sides) {
            this.sides = sides;
        }

        int sides() {
            return sides;
        }
    }

    public int exercise(Shape shape) {
        if (shape instanceof Circle circle) {
            return circle.radius() * circle.radius();
        }
        return switch (shape.getClass().getSimpleName()) {
            case "Rectangle" -> {
                Rectangle rectangle = (Rectangle) shape;
                yield rectangle.area();
            }
            case "Polygon" -> ((Polygon) shape).sides();
            default -> 0;
        };
    }

    public List<String> describe(List<Shape> shapes) {
        record NamedShape(String name, int score) {
        }

        var result = new ArrayList<String>();
        for (Shape shape : shapes) {
            NamedShape named = new NamedShape(
                    shape.getClass().getSimpleName(),
                    exercise(shape));
            result.add(named.name() + ":" + named.score());
        }
        return List.copyOf(result);
    }

    public String banner() {
        return """
                Java 17
                syntax
                """.strip();
    }
}
