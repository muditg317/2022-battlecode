package sagerunning.containers;

public class FastQueue<T> {
  private T[] array;
  private int front;
  private int size;
  private int iter;

  public FastQueue(int arraySize) {
    array = (T[]) new Object[arraySize];
  }

  public void push(T item) {
    array[(front + (size++)) % array.length] = item;
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

  public int size() {
    return size;
  }

  public void clear() {
    size = 0;
  }

  public T getFront() {
    return array[front % array.length];
  }

  public int startIter() {
    iter = front;
    return size;
  }

  public T next() {
    return array[iter++ % array.length];
  }
}
