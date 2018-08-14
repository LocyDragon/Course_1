package com.locydragon.test1;

import javassist.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;

/**
 * @author  LocyDragon
 */
public class CoreInject {
	public static final String craftPlayerClass = "/CraftPlayer";
	public static final String version = "";
	public static void premain(String agentArgs, Instrumentation inst) {
		//注意 在这里使用Bukkit.getServer() 会返回null 因为服务器还没有启动.
		System.out.println("注入sendMessage....");
		inst.addTransformer(new ClassFileTransformer() {
			@Override
			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
				if (className.endsWith(craftPlayerClass)) {
					ClassPool pool = ClassPool.getDefault();
					//获取Javassist的储存类的池
					try {
						Path path = Paths.get(Class.forName("org.bukkit.Bukkit").getProtectionDomain().getCodeSource().getLocation().toString().substring(6));
						//这样获取核心所在路径，这么做为了下面把核心导入ClassPool，不导入会找不到CraftPlayer类.
						String pathUtf = URLDecoder.decode(path.toFile().getPath(), "utf-8");
                        //路径转码 避免路径有中文而乱码报错
						pool.insertClassPath(pathUtf);
						//把核心导入ClassPool
						CtClass ctClass = pool.getCtClass(className.replace("/", "."));
						//获取CraftPlayer类
						CtMethod sendMsgMethod = ctClass.getDeclaredMethod("sendMessage", new CtClass[]{ pool.getCtClass("java.lang.String") });
						//获取发送信息的方法
						ctClass.removeMethod(sendMsgMethod);
						//避免方法重复
						StringBuilder code = new StringBuilder();
						//使用一个StringBuilder来储存源码
						code.append("{\n");
						code.append("if (message.equals(\"HelloWorld\")) { message = \"ByeWorld\"; } \n");
						code.append("($$);\n");
						//这里代表执行原有代码
						code.append("}\n");
						sendMsgMethod.setBody(code.toString());
						ctClass.addMethod(sendMsgMethod);
						return ctClass.toBytecode();
					} catch (ClassNotFoundException | NotFoundException | CannotCompileException | IOException exc) {
						exc.printStackTrace();
					}
				}
				return null;
				//需要return一个字节码，byte[]类型，如果没有要返回的请返回null.
			}
		});
	}
}
