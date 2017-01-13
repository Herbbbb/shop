package cn.itcast.shop.order.action;

import java.io.IOException;
import java.util.Date;

import org.apache.struts2.ServletActionContext;

import cn.itcast.shop.cart.vo.Cart;
import cn.itcast.shop.cart.vo.CartItem;
import cn.itcast.shop.order.service.OrderService;
import cn.itcast.shop.order.vo.Order;
import cn.itcast.shop.order.vo.OrderItem;
import cn.itcast.shop.user.vo.User;
import cn.itcast.shop.utils.PageBean;
import cn.itcast.shop.utils.PaymentUtil;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ModelDriven;

/**
 * 订单Action类
 * 
 * @author 传智.郭嘉
 * 
 */
public class OrderAction extends ActionSupport implements ModelDriven<Order> {
	// 模型驱动使用的对象
	private Order order = new Order();

	public Order getModel() {
		return order;
	}
	
	private String pd_FrpId;
	private String r3_Amt;
	
	public void setR3_Amt(String r3Amt) {
		r3_Amt = r3Amt;
	}

	public void setPd_FrpId(String pdFrpId) {
		pd_FrpId = pdFrpId;
	}

	private Integer page;
	private String r6_Order;
	

	public void setR6_Order(String r6Order) {
		r6_Order = r6Order;
	}

	public void setPage(Integer page) {
		this.page = page;
	}


	// 注入OrderService
	private OrderService orderService;

	public void setOrderService(OrderService orderService) {
		this.orderService = orderService;
	}

	//跳转订单页面
	public String save(){
		//获取到session中的购物车
		Cart cart = (Cart)ActionContext.getContext().getSession().get("cart");
		//封装order对象,用于存储到数据库
		if(cart!=null){
			order.setTotal(cart.getTotal());
		}else{
			this.addActionError("没有购物,需要先购物才能提交订单!");
			return "msg";
		}
		order.setOrdertime(new Date());
		order.setState(1);
		//获取到登录的用户对象用于取出用户信息
		User user = (User)ActionContext.getContext().getSession().get("existUser");
		if(user!=null){
			order.setUser(user);
		}else{
			this.addActionError("没有登录,需要先登录才能提交订单!");
			return "login";
		}
		//从取得的购物车中的购物项中遍历数据然后赋值给订单项以用于将订单项存储到数据库中
		for(CartItem cartItem:cart.getCartItems()){
			OrderItem orderItem = new OrderItem();
			orderItem.setCount(cartItem.getCount());
			orderItem.setSubtotal(cartItem.getSubTotal());
			orderItem.setOrder(order);
			orderItem.setProduct(cartItem.getProduct());
			//向订单中添加每次遍历得到的订单项
			order.getOrderItems().add(orderItem);
		}
		//调用orderService将订单对象和订单项对象都存储到数据库中
		orderService.save(order);
		//订单插入数据库中后还需要清空购物车
		cart.clearCart();
		return "saveOrder";
	}
	
	//根据用户的ID查询当前登录用户的所有订单
	public String findByUid(){
		User existUser = (User)ActionContext.getContext().getSession().get("existUser");
		Integer uid = existUser.getUid();
		PageBean<Order> pageBean = orderService.findByUid(uid,page);
		ActionContext.getContext().getValueStack().set("pageBean", pageBean);
		return "findByUidSuccess";
	}
	
	//在订单列表页面中点击未付款订单跳转到订单页面,需要在数据库中根据传入的oid查找指定订单并且显示到订单页面用于用户付款
	public String findByOid(){
		order = orderService.findByOid(order.getOid());
		return "findByOidSuccess";
	}
	
	public String payOrder() throws IOException{
		//根据oid查询即将付款的订单,并且将地址姓名和电话等信息一起存入到Order表中
		Order currOrder = orderService.findByOid(order.getOid());
		currOrder.setAddr(order.getAddr());
		currOrder.setName(order.getName());
		currOrder.setPhone(order.getPhone());
		orderService.update(currOrder);
		
		//支付订单代码
		String p0_Cmd = "Buy"; // 业务类型:
		String p1_MerId = "10001126856";// 商户编号:
		String p2_Order = order.getOid().toString();// 订单编号:
		String p3_Amt = "0.01"; // 付款金额:
		String p4_Cur = "CNY"; // 交易币种:
		String p5_Pid = ""; // 商品名称:
		String p6_Pcat = ""; // 商品种类:
		String p7_Pdesc = ""; // 商品描述:
		String p8_Url = "http://192.168.1.101:8080/shop/order_callBack.action"; // 商户接收支付成功数据的地址:
		String p9_SAF = ""; // 送货地址:
		String pa_MP = ""; // 商户扩展信息:
		String pd_FrpId = this.pd_FrpId;// 支付通道编码:
		String pr_NeedResponse = "1"; // 应答机制:
		String keyValue = "69cl522AV6q613Ii4W6u8K6XuW8vM1N6bFgyv769220IuYe9u37N4y7rI4Pl"; // 秘钥
		String hmac = PaymentUtil.buildHmac(p0_Cmd, p1_MerId, p2_Order, p3_Amt,
				p4_Cur, p5_Pid, p6_Pcat, p7_Pdesc, p8_Url, p9_SAF, pa_MP,
				pd_FrpId, pr_NeedResponse, keyValue); // hmac
		// 向易宝发送请求:
		StringBuffer sb = new StringBuffer("https://www.yeepay.com/app-merchant-proxy/node?");
		sb.append("p0_Cmd=").append(p0_Cmd).append("&");
		sb.append("p1_MerId=").append(p1_MerId).append("&");
		sb.append("p2_Order=").append(p2_Order).append("&");
		sb.append("p3_Amt=").append(p3_Amt).append("&");
		sb.append("p4_Cur=").append(p4_Cur).append("&");
		sb.append("p5_Pid=").append(p5_Pid).append("&");
		sb.append("p6_Pcat=").append(p6_Pcat).append("&");
		sb.append("p7_Pdesc=").append(p7_Pdesc).append("&");
		sb.append("p8_Url=").append(p8_Url).append("&");
		sb.append("p9_SAF=").append(p9_SAF).append("&");
		sb.append("pa_MP=").append(pa_MP).append("&");
		sb.append("pd_FrpId=").append(pd_FrpId).append("&");
		sb.append("pr_NeedResponse=").append(pr_NeedResponse).append("&");
		sb.append("hmac=").append(hmac);
		
		// 重定向:向易宝出发:
		ServletActionContext.getResponse().sendRedirect(sb.toString());
		return NONE;
	}
	
	
	// 付款成功后跳转回来的路径:
	public String callBack(){
		// 修改订单的状态:
		Order currOrder = orderService.findByOid(Integer.parseInt(r6_Order));
		// 修改订单状态为2:已经付款:
		currOrder.setState(2);
		orderService.update(currOrder);
		this.addActionMessage("支付成功!订单编号为: "+r6_Order +" 付款金额为: "+r3_Amt);
		return "msg";
	}
	
	
	//前台确认收货方法
	public String updateState(){
		Order currOrder = orderService.findByOid(order.getOid());
		currOrder.setState(4);
		orderService.update(currOrder);
		return "updateStateSuccess";
	}
	
}
