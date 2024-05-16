package ProjectTwo;

// Class definition for a Heap
class Heap {
    private Node[] Heap; // Array to store the elements of the heap
    private int size;        // Current number of elements in the heap
    private int maxSize;     // Maximum size of the heap

    // Constructor to initialize the heap with a maximum size
    public Heap(int maxSize) {
        this.maxSize = maxSize;
        this.size = 0;
        Heap = new Node[maxSize + 1];
        Node dummy = new Node(0); // A dummy node with minimum frequency, used as a sentinel
        Heap[0] = dummy;
    }

    // Function to maintain the heap property starting from a given position
    private void minHeapify(int pos) {
        if (!isLeaf(pos)) { // Check if the current node is not a leaf node
            boolean hasLeftChild = leftChild(pos) <= size && Heap[leftChild(pos)] != null;
            boolean hasRightChild = rightChild(pos) <= size && Heap[rightChild(pos)] != null;

            if (hasLeftChild) {
                int smallestChildPos = leftChild(pos);
                // Check if the right child is smaller than the left child
                if (hasRightChild && Heap[rightChild(pos)].getFreq() < Heap[leftChild(pos)].getFreq()) {
                    smallestChildPos = rightChild(pos);
                }
                // Swap with the smaller child and continue heapifying if necessary
                if (Heap[pos].getFreq() > Heap[smallestChildPos].getFreq()) {
                    swap(pos, smallestChildPos);
                    minHeapify(smallestChildPos);
                }
            }
        }
    }

    // Function to insert a new element into the heap
    public void insert(Node element) {
        if (size >= maxSize) {
            return; // Do nothing if the heap is already full
        }
        
        Heap[++size] = element; // Place the element at the end of the heap
        int current = size;
        // Adjust the position of the newly added element to maintain the heap property
        while (Heap[current].getFreq() < Heap[parent(current)].getFreq()) {
            swap(current, parent(current));
            current = parent(current);
        }
    }

    // Function to remove and return the minimum element from the heap
    public Node remove() {
    	Node popped = Heap[1]; // The root of the heap, which is the minimum element
        Heap[1] = Heap[size--]; // Replace the root with the last element and decrease the size
        minHeapify(1); // Restore the heap property
        return popped;
    }

    // Getter to obtain the current size of the heap
	public int getSize() {
		return size;
	}
	
    // Helper function to get the index of the parent of a given node
	private int parent(int pos) {
        return pos / 2;
    }

    // Helper function to get the index of the left child of a given node
    private int leftChild(int pos) {
        return (2 * pos);
    }

    // Helper function to get the index of the right child of a given node
    private int rightChild(int pos) {
        return (2 * pos) + 1;
    }

    // Helper function to check if a given node is a leaf
    private boolean isLeaf(int pos) {
        return pos > (size / 2) && pos <= size;
    }

    // Helper function to swap two elements in the heap
    private void swap(int fpos, int spos) {
        Node tmp;
        tmp = Heap[fpos];
        Heap[fpos] = Heap[spos];
        Heap[spos] = tmp;
    }
    
}
