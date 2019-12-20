package com.paypal.jzhang13.reactorNIO;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public class ReactorServer {
	private static final int PORT_NUMBER = 12345;

	public static void main(String[] args) throws IOException {
		ExecutorService workers = Executors.newFixedThreadPool(4);
		ServerSocketChannel server = ServerSocketChannel.open();
		server.socket().bind(new InetSocketAddress(PORT_NUMBER));
		server.socket().setReuseAddress(true);
		server.configureBlocking(false);

		Selector selector = Selector.open();
		SelectionKey target = server.register(selector, SelectionKey.OP_ACCEPT);
		target.attach(new Acceptor(server,selector));
		System.out.println("Echo server starts on port:"+PORT_NUMBER);
		while (true) {
			int channelCount = selector.select();
			if (channelCount > 0) {
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> iterator = keys.iterator();
				while (iterator.hasNext()) {
					SelectionKey key = iterator.next();
					iterator.remove();
					if (key.isAcceptable()) {
						Acceptor acceptor = (Acceptor)key.attachment();
						acceptor.run();
//						SocketChannel socket = server.accept();
//						socket.configureBlocking(false);
//						socket.register(selector, SelectionKey.OP_READ, new Handler(socket));
					}
					else if(key.isReadable()) {
						Handler h = (Handler)key.attachment();
						workers.submit(h);
					}
				}
				
			}
		}
	}
}

class Acceptor implements Runnable{
	
	private ServerSocketChannel server;
	private Selector selector;
	//int reactorCount = Runtime.getRuntime().availableProcessors();
	int reactorCount = 4;
	ChildReactor[] reactors = new ChildReactor[reactorCount];
	ExecutorService workers = Executors.newFixedThreadPool(8);
	ExecutorService reactorPool = Executors.newFixedThreadPool(reactorCount);
		
	public Acceptor(ServerSocketChannel server, Selector selector) throws IOException {
		this.server = server;
		this.selector = selector;
//		for(int i=0;i<reactorCount;i++) {
//			reactors[i] = new ChildReactor();
//			reactorPool.submit(reactors[i]);
//		}
	}
	@Override
	public void run() {
		try {

			SocketChannel socket = server.accept();
	        SocketAddress remoteAddr = socket.getRemoteAddress();
	        System.out.println("Connected to: " + remoteAddr);
			this.register(socket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	private void register(SocketChannel socket) throws IOException {
		int randomNum = ThreadLocalRandom.current().nextInt(0, reactorCount);
		//ChildReactor reactor = reactors[randomNum];
		socket.configureBlocking(false);
		socket.register(selector, SelectionKey.OP_READ, new Handler(socket));
		System.out.println("test");
		//reactor.register(socket);
	}
	
	class ChildReactor implements Runnable{
		private Selector selector;
		
		public ChildReactor() throws IOException {
			selector = SelectorProvider.provider().openSelector();;
		}

		public synchronized void register(SocketChannel socket) throws IOException {
			if (socket != null) {
//				synchronized (socket) {
					socket.configureBlocking(false);
					selector.wakeup();
					socket.register(selector, SelectionKey.OP_READ, new Handler(socket));
					System.out.println("test");
				//}
			}
		}
		@Override
		public void run() {
			while(true) {
				try {
					this.selector.select();
					Set<SelectionKey> keys = this.selector.selectedKeys(); 
					Iterator<SelectionKey> ite = keys.iterator();
					while(ite.hasNext()) {
						SelectionKey key = ite.next();
						ite.remove();
						if(key.isReadable()) {
							Handler h = (Handler)key.attachment();
							workers.submit(h);
						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}			
			}
		}	
	}
	
			
}

class Handler implements Runnable {
	private SocketChannel socket;
	private ByteBuffer buffer = ByteBuffer.allocate(1024);

	public Handler(SocketChannel socket) {
		this.socket = socket;
	}

	@Override
	public void run() {
		SocketChannel client = socket;
		StringBuffer strb = new StringBuffer();
//		int numRead = 0;
//		try {
//			if ((numRead = client.read(buffer)) < 0) {
//				client.close();
//			} else {
//		        byte[] data = new byte[numRead];
////		        System.arraycopy(buffer.array(), 0, data, 0, numRead);
////		        System.out.println("Got: " + new String(data, "US-ASCII"));
//				buffer.flip(); // read from the buffer
//				/*
//				 * byte[] received = new byte[buffer.remaining()]; buffer.get(received);
//				 * buffer.clear(); // write into the buffer buffer.put(received); buffer.flip();
//				 * // read from the buffer
//				 */
//				client.write(buffer);
//				buffer.compact();
//				buffer.clear(); // write into the buffer }
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		try {
			if(client.read(buffer) > 0) {  
				  
				buffer.flip();  
			    byte[] bytes = new byte[buffer.remaining()];  
			    buffer.get(bytes);  
			    buffer.clear();  
			    String msg = new String(bytes);  
			    System.out.println("echo read message:" + msg);
			    strb.append(msg);  
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}  
        ByteBuffer writeBuffer = ByteBuffer.wrap(strb.toString().getBytes());  
        //LOGGER.info("{}", writeBuffer.position());  
        //LOGGER.info("{}", writeBuffer.limit());  
        try {
			client.write(writeBuffer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
    }  
}

