package cn.archko.pdf.listeners;

/**
 * 数据查询接口.所有的数据查询都可以用这个,因为返回的是多个元素.
 *
 * @author: archko 14/11/5 :17:33
 */
public interface DataListener {

    public void onSuccess(Object... args);

    public void onFailed(Object... args);
}
