// 导入SparkMD5库
importScripts('https://cdnjs.cloudflare.com/ajax/libs/spark-md5/3.0.2/spark-md5.min.js');

// 接收主线程消息
self.onmessage = function(e) {
  // 获取传递的数据
  const arrayBuffer = e.data.arrayBuffer;
  const fileSize = e.data.fileSize;
  
  // 为大文件分片处理
  if (fileSize > 10 * 1024 * 1024) { // 如果大于10MB，分片处理
    processLargeFile(arrayBuffer, fileSize);
  } else {
    // 直接处理小文件
    const spark = new SparkMD5.ArrayBuffer();
    spark.append(arrayBuffer);
    const md5 = spark.end();
    self.postMessage({ md5 });
  }
};

// 处理大文件
function processLargeFile(arrayBuffer, fileSize) {
  const chunkSize = 2 * 1024 * 1024; // 2MB分片
  const chunks = Math.ceil(fileSize / chunkSize);
  let currentChunk = 0;
  const spark = new SparkMD5.ArrayBuffer();
  
  // 处理每个分片
  function processChunk() {
    const start = currentChunk * chunkSize;
    const end = Math.min(fileSize, start + chunkSize);
    
    // 从ArrayBuffer中切出一片
    const chunk = arrayBuffer.slice(start, end);
    
    // 添加到MD5计算
    spark.append(chunk);
    currentChunk++;
    
    // 发送进度
    self.postMessage({
      progress: Math.floor((currentChunk / chunks) * 100)
    });
    
    if (currentChunk < chunks) {
      // 使用setTimeout避免阻塞
      setTimeout(processChunk, 0);
    } else {
      // 完成所有分片
      const md5 = spark.end();
      self.postMessage({ md5 });
    }
  }
  
  // 开始处理
  processChunk();
} 