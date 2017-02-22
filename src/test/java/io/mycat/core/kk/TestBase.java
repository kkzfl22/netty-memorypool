package io.mycat.core.kk;

public class TestBase {
    
    
    public static void main(String[] args) {
        System.out.println(String.valueOf(Integer.SIZE - 1 ));
      System.out.println(String.valueOf(Integer.numberOfLeadingZeros(8192)));
      System.out.println(String.valueOf(Integer.SIZE - 1  - Integer.numberOfLeadingZeros(8192)));
     System.out.println((((long) Integer.MAX_VALUE + 1) / 2)/1024/1024);
    }
    
}
