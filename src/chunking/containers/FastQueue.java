package chunking.containers;

public class FastQueue<T> {
  private T[] array;
  private int front;
  private int size;

  public FastQueue(int arraySize) {
    array = (T[]) new Object[arraySize];
  }

  public void push(T message) {
    array[(front + (size++)) % array.length] = message;
  }

  public T popFront() {
    size--;
    return array[front++ % array.length];
  }

  public void removeFront() {
    size--; front++;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  public int getSize() {
    return size;
  }

  public void reset() {
    size = 0;
  }

  public T getFrontMessage() {
    return array[front % array.length];
  }
}
