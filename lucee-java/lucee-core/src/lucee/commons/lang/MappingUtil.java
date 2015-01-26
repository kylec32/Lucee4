/**
 *
 * Copyright (c) 2014, the Railo Company Ltd. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 **/
package lucee.commons.lang;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lucee.commons.io.IOUtil;
import lucee.commons.io.res.Resource;
import lucee.commons.io.res.filter.DirectoryResourceFilter;
import lucee.commons.io.res.filter.ExtensionResourceFilter;
import lucee.runtime.Mapping;
import lucee.runtime.MappingImpl;
import lucee.runtime.PageContext;
import lucee.runtime.PageSource;
import lucee.runtime.config.Config;
import lucee.runtime.config.ConfigWebUtil;
import lucee.runtime.engine.ThreadLocalPageContext;
import lucee.transformer.bytecode.util.ASMUtil;
import lucee.transformer.bytecode.util.SourceNameClassVisitor.SourceInfo;
import lucee.commons.io.res.filter.ResourceFilter;

public class MappingUtil {
	//private static final ResourceFilter EXT=new ExtensionResourceFilter(".cfc");
	//private static final ResourceFilter DIR_OR_EXT=new OrResourceFilter(new ResourceFilter[]{DirectoryResourceFilter.FILTER,EXT});

	
	public static PageSource searchMappingRecursive(Mapping mapping, String name, boolean onlyCFC) {
		if(name.indexOf('/')==-1) { // TODO handle this as well?
			Config config = mapping.getConfig();
			ExtensionResourceFilter ext =null;
			if(onlyCFC) ext=new ExtensionResourceFilter(new String[]{config.getCFCExtension()},true,true);
			else {
				ext=new ExtensionResourceFilter(config.getCFMLExtensions(),true,true);
				ext.addExtension(config.getCFCExtension());
			}
			
			if(mapping.isPhysicalFirst()) {
				PageSource ps = searchPhysical(mapping,name,ext);
				if(ps!=null) return ps;
				ps=searchArchive(mapping,name,onlyCFC);
				if(ps!=null) return ps;
			}
			else {
				PageSource ps=searchArchive(mapping,name,onlyCFC);
				if(ps!=null) return ps;
				ps = searchPhysical(mapping,name,ext);
				if(ps!=null) return ps;
			}
		}
		return null;
	}

	private static PageSource searchArchive(Mapping mapping, String name, boolean onlyCFC) {
		Resource archive = mapping.getArchive();
		if(archive!=null && archive.isFile()) {
			ClassLoader cl = mapping.getClassLoaderForArchive();
			ZipInputStream zis = null;
			try{
				zis = new ZipInputStream(archive.getInputStream());
				ZipEntry entry;
				Class clazz;
				while((entry=zis.getNextEntry())!=null){
					if(entry.isDirectory() || !entry.getName().endsWith(".class")) continue;
					clazz=toClass(cl,entry.getName());
					
					if(clazz==null) continue;
					SourceInfo srcInf = ASMUtil.getSourceInfo(mapping.getConfig(),clazz,onlyCFC);
					if(name.equalsIgnoreCase(srcInf.name)) {
						PageSource ps = mapping.getPageSource(srcInf.relativePath);
						//Page page = ((PageSourceImpl)ps).loadPage(pc,(Page)null);
						return ps;
					}
				}
				
				
				
			}
			catch(IOException ioe) {
				ioe.printStackTrace();
			}
			finally {
				IOUtil.closeEL(zis);
			}
			
		}
		// TODO Auto-generated method stub
		return null;
	}
	
	private static Class toClass(ClassLoader cl,String name) {
		name=name.replace('/', '.').substring(0,name.length()-6);
		try {
			return cl.loadClass(name);
		}
		catch (ClassNotFoundException e) {}
		return null;
	}

	
	
	
	

	private static PageSource searchPhysical(Mapping mapping, String name, ResourceFilter filter) {
		Resource physical = mapping.getPhysical();
		if(physical!=null) {
			String _path=searchPhysical(mapping.getPhysical(), null,name,filter,true);
			
			if(_path!=null) {
				PageSource ps = mapping.getPageSource(_path);
				//Page page = ((PageSourceImpl)ps).loadPage(pc,(Page)null);
				return ps;
			}
		}
		return null;
	}
	
	private static String searchPhysical(Resource res, String dir,String name, ResourceFilter filter, boolean top) {
		if(res.isFile()) {
			if(res.getName().equalsIgnoreCase(name)) {
				return dir+res.getName();
			}
		}
		else if(res.isDirectory()) {
			Resource[] _dir = res.listResources(top?DirectoryResourceFilter.FILTER:filter);
			if(_dir!=null){
				if(dir==null) dir="/";
				else dir=dir+res.getName()+"/";
				String path;
				for(int i=0;i<_dir.length;i++){
					path=searchPhysical(_dir[i],dir, name,filter,false);
					if(path!=null) return path;
				}
			}
		}
		
		return null;
	}
	
	public static SourceInfo getMatch(PageContext pc, StackTraceElement trace) {
		return getMatch(pc,null, trace);
		
	}

	public static SourceInfo getMatch(Config config, StackTraceElement trace) {
		return getMatch(null,config, trace);
	}
	
	public static SourceInfo getMatch(PageContext pc,Config config, StackTraceElement trace) {
		if(pc==null && config==null)
			config=ThreadLocalPageContext.getConfig();
		if(trace.getFileName()==null) return null;
		
		//PageContext pc = ThreadLocalPageContext.get();
		Mapping[] mappings = pc!=null? ConfigWebUtil.getAllMappings(pc):ConfigWebUtil.getAllMappings(config);
		if(pc!=null) config=pc.getConfig();
		
		Mapping mapping;
		Class clazz;
		for(int i=0;i<mappings.length;i++){
			mapping=mappings[i];
			//print.e("virtual:"+mapping.getVirtual()+"+"+trace.getClassName());
			// look for the class in that mapping
			clazz=((MappingImpl)mapping).loadClass(trace.getClassName());
			if(clazz==null) continue;
			
			// classname is not distinct, because of that we must check class content
			try {
				SourceInfo si = ASMUtil.getSourceInfo(config, clazz, false);
				if(si!=null && trace.getFileName()!=null && trace.getFileName().equals(si.absolutePath))
					return si;
			}
			catch (IOException e) {}
			
			
		}
		return null;
	}
}
