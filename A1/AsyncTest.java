import java.util.ArrayList;

public class AsyncTest {

  public class GETThread extends Thread{
    private String url;

    public GETThread(String url) {
      this.url = url;
    }

    public void run() {
      HTTPResponse hr = GET.make(this.url).send();
      System.out.println(String.format("%s\n%s", hr.getHeaders(), hr.getBody()));
    }
  }

  public class POSTThread extends Thread{
    private String url;
    private String body;

    public POSTThread(String url, String body) {
      this.url = url;
      this.body = body;
    }

    public void run() {
      HTTPResponse hr = POST.make(this.url).addToBody(this.body).send();
      System.out.println(String.format("%s\n%s", hr.getHeaders(), hr.getBody()));
    }
  }


  public static void main(String[] args) {
    for(int j = 0; j<args.length; j++) {
      System.out.println(args[j]);
    }
    try {
      if (args[0].equals("readtest")) {
        ArrayList<Thread> threads = new ArrayList<Thread>();
        AsyncTest a = new AsyncTest();
        for(int i = 0; i<100; i++) {
          threads.add(a.new GETThread("http://localhost/testfile"));
        }
        for(Thread t:threads) {
          t.start();
        }
        for(Thread t:threads){
          t.join();
        }
      } else if (args[0].equals("write")) {
        ArrayList<Thread> threads = new ArrayList<Thread>();
        AsyncTest a = new AsyncTest();
        for(int i = 0; i<20; i++) {
          threads.add(a.new POSTThread("http://localhost/testfile", "Here is the content of thread " + i));
        }
        for(Thread t:threads) {
          t.start();
        }
        for(Thread t:threads){
          t.join();
        }
      } else if (args[0].equals("readwrite")) {
        ArrayList<Thread> threads = new ArrayList<Thread>();
        AsyncTest a = new AsyncTest();
        for(int i = 0; i<20; i+=2) {
          threads.add(a.new POSTThread("http://localhost/testfile", "Here is the content of thread " + i));
          threads.add(a.new GETThread("http://localhost/testfile"));
        }
        for(Thread t:threads) {
          t.start();
        }
        for(Thread t:threads){
          t.join();
        }
      }
    }catch(Exception e){
      System.exit(0);
    }

  }
}