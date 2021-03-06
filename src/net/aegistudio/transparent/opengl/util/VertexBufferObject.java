package net.aegistudio.transparent.opengl.util;

import java.nio.Buffer;

import net.aegistudio.transparent.util.Bindable;
import net.aegistudio.transparent.util.BindingFailureException;
import net.aegistudio.transparent.util.Scoped;

import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.GLContext;

/**
 * This class encapsulates the OpenGL vertex buffer object (VBO), which is used to store
 * rendering data in VRAM for higher efficiency.
 * @author aegistudio
 */

public class VertexBufferObject implements Scoped, Bindable
{
	private int bufferId = 0;
	private final int bufferSize;
	
	private final int bufferTarget;
	private final int bufferUsage;
	
	private EnumDataType bufferType;
	private Buffer buffer;
	
	/**
	 * Creates a VBO java object and pre-install some data, not buffer the data in VRAM.
	 * @param bufferTarget The target buffer of this VBO, like GL_VERTEX_ARRAY, GL_PIXEL_UNPACK_BUFFER, etc.
	 * @param bufferUsage The usage of this VBO, like GL_STATIC_DRAW, GL_DYNAMIC_COPY, etc.
	 * @param buffer The target array to buffer, the buffer array type could only be byte, int, float, double, short and their wrapper class.
	 */
	public VertexBufferObject(EnumBufferTarget bufferTarget, EnumBufferUsage bufferUsage, Object buffer)
	{
		if(buffer == null) throw new IllegalArgumentException("Buffer should not be null!");
		this.bufferTarget = bufferTarget.stateId;
		this.bufferUsage = bufferUsage.stateId;
		
		buffer = BufferHelper.convertToArrayIfNecessary(buffer);
		Class<?> clz = BufferHelper.getCertainClass(buffer);
		this.bufferSize = BufferHelper.getLength(buffer);

		if(clz == null) throw new IllegalArgumentException("Unable to create buffer for given type!");
		this.bufferType = EnumDataType.getDataType(clz);
		if(this.bufferType == null) throw new IllegalArgumentException("Unable to create buffer for given type!");
		
		this.buffer = BufferHelper.getBufferProcessor(bufferType).makeBuffer(buffer);
	}
	
	/**
	 * Creates a vertex buffer object in the VRAM and returns the created id.
	 * @return the created VBO indentity.
	 */
	public int create()
	{
		if(this.bufferId == 0)
		{
			if(!GLContext.getCapabilities().GL_ARB_vertex_buffer_object) throw new FeatureNotSupportedException("vertex buffer object");
			this.bufferId = ARBVertexBufferObject.glGenBuffersARB();
			if(this.bufferId == 0) throw new BindingFailureException("Fail to create space for the vertex buffer object!");
			
			ARBVertexBufferObject.glBindBufferARB(bufferTarget, bufferId);
			BufferHelper.getBufferProcessor(bufferType).bufferData(bufferTarget, buffer, bufferUsage);
			ARBVertexBufferObject.glBindBufferARB(bufferTarget, 0);
		}
		return this.bufferId;
	}
	
	/**
	 * Set the current vertex buffer object in use.
	 */
	public void bind()
	{
		if(bufferId == 0) throw new BindingFailureException("You must create the buffer before binding it!");
		ARBVertexBufferObject.glBindBufferARB(bufferTarget, bufferId);
	}
	
	/**
	 * Set the current vertex buffer object not in use.
	 */
	public void unbind()
	{
		if(bufferId == 0) throw new BindingFailureException("You must create the buffer before unbinding it!");
		ARBVertexBufferObject.glBindBufferARB(bufferTarget, 0);
	}
	
	/**
	 * Substitute certain data in the vertex buffer object that's in the VRAM.
	 */
	public void subst(int position, Object obj)
	{
		if(this.bufferId != 0)
		{
			if(position < 0 || position >= bufferSize)
				throw new IllegalArgumentException("The buffer space to change is out of the permitted position!");
			
			int objectLength = BufferHelper.getLength(obj);
			if(position + objectLength > bufferSize)
				throw new IllegalArgumentException("The buffer space to change is out of the permitted position!");
			
			obj = BufferHelper.convertToArrayIfNecessary(obj);
			Class<?> clz = BufferHelper.getCertainClass(obj);
			
			EnumDataType dataType = EnumDataType.getDataType(clz);
			if((dataType == null) || (dataType.inferGLType() != this.bufferType.inferGLType()))
				throw new IllegalArgumentException("Mismatch between buffer component type and the type of element to change!");
			
			
			Buffer subbuffer = BufferHelper.getBufferProcessor(dataType).makeBuffer(obj);
			ARBVertexBufferObject.glBindBufferARB(bufferTarget, bufferId);
			BufferHelper.getBufferProcessor(dataType).substData(bufferTarget, subbuffer, position);
			ARBVertexBufferObject.glBindBufferARB(bufferTarget, 0);
		}
		else throw new BindingFailureException("You must create the buffer before subst it!");
	}
	
	/**
	 * Get the array component type of this buffer.
	 * @return the array component type.
	 */
	public EnumDataType getBufferType()
	{
		return this.bufferType;
	}
	
	/**
	 * Remove / release the vertex buffer object on the VRAM.
	 */
	public void destroy()
	{
		if(this.bufferId != 0)
		{
			ARBVertexBufferObject.glDeleteBuffersARB(bufferId);
			this.bufferId = 0;
		}
	}
	
	public int getLength()
	{
		return this.bufferSize;
	}
}
