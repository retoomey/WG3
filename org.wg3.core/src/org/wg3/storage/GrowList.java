package org.wg3.storage;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.wg3.log.Logger;
import org.wg3.log.LoggerFactory;

/**
 *
 * @author Robert Toomey
 *
 * This list is highly specialized linked list for synchronization. Basically it
 * allows adding to the end of it within one thread, while reading up-to a
 * previous size in another. The java classes do so much checking/etc. and are
 * very generalized, so that this helps with speed.
 *
 * It doesn't allow changing items. It grows only. It can only be iterated
 * forward...
 *
 * It is used so that a builder thread can append it it, while an openGL thread
 * can draw from it at the same time.
 *
 * Note that the objects stored inside the list are not synchronized
 *
 */
public class GrowList<E> {

	@SuppressWarnings("unused")
	private final static Logger LOG = LoggerFactory.getLogger(GrowList.class);

	/** Create a node...single linked list.  We could save a bit of
	 * memory by 'blocking' items into chunks...might do that later
	 * @param <E2> 
	 */
	private static class node<E2> {

		node(E2 item) {
			item0 = item;
		}
		E2 item0;
		node<E2> next;
	}

	/**
	 * Note the iterator itself is not thread-safe. You should create one
	 * everytime you want to read from the list.  In other words, don't
	 * use the same iterator object in two different threads.
	 */
	public class Itr implements Iterator<E> {

		private int sizeAtCreation;
		private int cursor = -1;
		private node<E> at;

		public Itr() {

			// Note the only time we need to synchronize is at
			// creation.  The root might change or the last node might
			// add a next pointer, but we don't care.  We keep track of
			// how many we have gone through....

			// get size first, since root is made non-null
			// BEFORE size increase...
			sizeAtCreation = getSize();
			// 2nd call not synched with above, but that's ok
			at = root;
		}

		@Override
		public boolean hasNext() {
			return ((cursor + 1) < sizeAtCreation);
		}

		@Override
		public E next() throws NoSuchElementException {                
			E item = null;
			if (at != null) {
				// First call, use root, otherwise move forward 1
				if (cursor > -1){
					at = at.next;
				}
				// Get item if not-null
				if (at != null) {
					item = at.item0;
				}
			}
			cursor++;
			if (item == null) {
				throw new NoSuchElementException();
			}
			return item;
		}

		@Override
		public void remove() {
			// not allowed
		}
	}
	private node<E> root;
	private node<E> last;
	private int size = 0;

	public Iterator<E> iterator() {
		return new Itr();
	}

	/**
	 * Add to the list
	 */
	public boolean add(E e) {
		node<E> n = new node<E>(e);

		// Change root before size, since iterator might
		// be in different thread....worst case you have the
		// new root but a smaller size...
		if (root == null) { // first node....
			root = n;
			last = n;
		} else {
			// Add to end of list....note done before size
			last.next = n;
			last = n;
		}
		//setSize(size + 1);
		increaseSize();
		return true;
	}

	// You don't want the size, you want to use the iterator
	// and next...the size is changing in another thread..
	private synchronized int getSize() {
		return size;
	}

	private synchronized void increaseSize() {
		size +=1;
	}
}
