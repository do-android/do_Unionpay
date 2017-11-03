package doext.implement;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;

import com.unionpay.UPPayAssistEx;

import core.DoServiceContainer;
import core.helper.DoJsonHelper;
import core.interfaces.DoActivityResultListener;
import core.interfaces.DoIPageView;
import core.interfaces.DoIScriptEngine;
import core.object.DoInvokeResult;
import core.object.DoSingletonModule;
import doext.define.do_Unionpay_IMethod;

/**
 * 自定义扩展SM组件Model实现，继承DoSingletonModule抽象类，并实现do_Unionpay_IMethod接口方法；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.getUniqueKey());
 */
public class do_Unionpay_Model extends DoSingletonModule implements do_Unionpay_IMethod, DoActivityResultListener {

	/*****************************************************************
	 * mMode参数解释： "00" - 启动银联正式环境 "01" - 连接银联测试环境
	 *****************************************************************/
	private String mMode = "01";
	private String verifyUrl;
	private DoIPageView doActivity;
	private DoIScriptEngine scriptEngine;
	private String callbackFuncName;

	public do_Unionpay_Model() throws Exception {
		super();
	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		//...do something
		return super.invokeSyncMethod(_methodName, _dictParas, _scriptEngine, _invokeResult);
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName,
	 *                    _invokeResult);
	 *                    参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
	 *                    获取DoInvokeResult对象方式new
	 *                    DoInvokeResult(this.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		if ("startPay".equals(_methodName)) {
			startPay(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		}
		return super.invokeAsyncMethod(_methodName, _dictParas, _scriptEngine, _callbackFuncName);
	}

	/**
	 * 支付；
	 * 
	 * @throws JSONException
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_callbackFuncName 回调函数名
	 */
	@Override
	public void startPay(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws JSONException {
		doActivity = _scriptEngine.getCurrentPage().getPageView();
		doActivity.registActivityResultListener(this);
		this.scriptEngine = _scriptEngine;
		this.callbackFuncName = _callbackFuncName;
		mMode = DoJsonHelper.getString(_dictParas, "mode", "");
		verifyUrl = DoJsonHelper.getString(_dictParas, "verifyUrl", "");
		final String _orderInfo = DoJsonHelper.getString(_dictParas, "orderInfo", "");

		Runnable payRunnable = new Runnable() {
			@Override
			public void run() {
				Message msg = mHandler.obtainMessage();
				msg.obj = _orderInfo;
				mHandler.sendMessage(msg);
			}
		};
		// 必须异步调用
		Thread unionpayThread = new Thread(payRunnable);
		unionpayThread.start();
	}

	private Handler mHandler = new Handler(new Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			String tn = "";
			if (msg.obj == null || ((String) msg.obj).length() == 0) {
				
			} else {
				tn = (String) msg.obj;
				/*************************************************
				 * 步骤2：通过银联工具类启动支付插件
				 ************************************************/
				UPPayAssistEx.startPay(DoServiceContainer.getPageViewFactory().getAppContext(), null, null, tn, mMode);
			}

			return false;

		}
	});
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		doActivity.unregistActivityResultListener(this);
		/*************************************************
		 * 步骤3：处理银联手机支付控件返回的支付结果
		 ************************************************/
		if (data == null) {
			return;
		}

		/*
		 * 支付控件返回字符串:success、fail、cancel 分别代表支付成功，支付失败，支付取消
		 */
		String str = data.getExtras().getString("pay_result");
		if (str.equalsIgnoreCase("success") && verifyUrl.equals("")) {
			fireResult(0, "success");
			return;
		}
		if (str.equalsIgnoreCase("success")) {

			// 支付成功后，extra中如果存在result_data，取出校验
			// result_data结构见c）result_data参数说明
			if (data.hasExtra("result_data")) {
				String result = data.getExtras().getString("result_data");
				try {
					JSONObject resultJson = new JSONObject(result);
					String sign = resultJson.getString("sign");
					// 验签证书同后台验签证书
					// 此处的verify，商户需送去商户后台做验签
					if (!verifyUrl.equals("")) {
						verify(sign, verifyUrl);
					}

				} catch (JSONException e) {
				}
			} else {
				// 未收到签名信息
				// 建议通过商户后台查询支付结果
				fireResult(0, "success");
			}

		} else if (str.equalsIgnoreCase("fail")) {
			fireResult(1, "fail");
		} else if (str.equalsIgnoreCase("cancel")) {
			fireResult(-1, "cancel");
		}

	}

	private void verify(final String sign64, final String url) {
		// 此处的verify，商户需送去商户后台做验签
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					requestPost(url, sign64);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	private void requestPost(String url, String params) throws Exception {

		HttpClient httpClient = getHttpClient(5000);
		BasicHttpResponse response;
		HttpPost post = new HttpPost(url);
		StringEntity se = new StringEntity(params, HTTP.UTF_8);
		se.setContentType("text/xml");
		post.setEntity(se);
		response = (BasicHttpResponse) httpClient.execute(post);
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode == 200) {
			fireResult(0, "success");
		} else {
			fireResult(1, "fail");
		}
	}

	private void fireResult(int statusCode, String content) {
		DoInvokeResult _invokeResult = new DoInvokeResult(do_Unionpay_Model.this.getUniqueKey());
		try {
			JSONObject _result = new JSONObject();
			_result.put("code", statusCode);
			_result.put("msg", content);
			_invokeResult.setResultNode(_result);
		} catch (Exception e) {
			DoServiceContainer.getLogEngine().writeError("do_Unionpay_Model pay \n\t", e);
		} finally {
			scriptEngine.callback(callbackFuncName, _invokeResult);
		}
	}

	private HttpClient getHttpClient(int timeOut) {
		HttpClient httpClient = null;
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null, null);
			SSLSocketFactory sf = new SSLSocketFactoryEx(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			HttpParams params = new BasicHttpParams();

			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.DEFAULT_CONTENT_CHARSET);
			HttpProtocolParams.setUseExpectContinue(params, true);

			ConnManagerParams.setTimeout(params, timeOut);
			HttpConnectionParams.setConnectionTimeout(params, timeOut);
			HttpConnectionParams.setSoTimeout(params, timeOut);
			SchemeRegistry schReg = new SchemeRegistry();
			schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			schReg.register(new Scheme("https", sf, 443));

			httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(params, schReg), params);

		} catch (Exception e) {
			e.printStackTrace();
			return new DefaultHttpClient();
		}
		return httpClient;
	}

	class SSLSocketFactoryEx extends SSLSocketFactory {
		SSLContext sslContext = SSLContext.getInstance("TLS");

		public SSLSocketFactoryEx(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
			super(truststore);
			sslContext.init(null, new TrustManager[] { new UnionTrustManager() }, null);
		}

		@Override
		public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
			return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
		}

		@Override
		public Socket createSocket() throws IOException {
			return sslContext.getSocketFactory().createSocket();
		}
	}

	class UnionTrustManager implements X509TrustManager {
		@Override
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		@Override
		public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
		}

		@Override
		public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
		}
	}
}