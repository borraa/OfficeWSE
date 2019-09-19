package listener;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import common.com.Trace;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

@DisallowConcurrentExecution
public class ListenerServer implements Job { 
	
	public void execute(JobExecutionContext context) throws JobExecutionException {
		try {
			JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
			final Trace trace 		= (Trace) jobDataMap.get("trace");
			final String encoding 	= (String) jobDataMap.get("encoding");
			int port 				= (int) jobDataMap.get("port");
	        
	        start_server(trace, port, encoding);
	        
	    } catch(Exception e) {
	    	return;
	    }
	}
	
	public boolean start_server(final Trace trace, int port, final String encoding) {
		
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();
    
		try {

			ServerBootstrap b = new ServerBootstrap();

			b.group(bossGroup, workerGroup)
			.channel(NioServerSocketChannel.class)
			.childHandler(new ChannelInitializer <SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) {
					ChannelPipeline p = ch.pipeline(); 
					p.addLast(new ServerHandler(trace, encoding));
				}
			});
			ChannelFuture f = b.bind(port).sync();
			f.channel().closeFuture().sync();
			return true;
		} catch(Exception e) {
			return false;
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}	
	}
}


