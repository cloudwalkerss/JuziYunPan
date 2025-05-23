/**
 * PPTX.js - PowerPoint 演示文稿预览库
 */
class PptxJS {
  constructor() {
    this.currentSlide = 1;
    this.totalSlides = 0;
    this.slides = [];
    this.container = null;
    this.scale = 1;
    this.jszip = null;
    this.presentation = null;
    this.slideWidth = 720; // 默认幻灯片宽度
    this.slideHeight = 540; // 默认幻灯片高度
    this.theme = null; // 主题样式
  }

  async load(url, headers = {}) {
    try {
      // 加载依赖库
      if (!window.JSZip) {
        await this.loadDependency('https://cdnjs.cloudflare.com/ajax/libs/jszip/3.10.1/jszip.min.js');
      }
      
      // 获取PPTX文件
      const response = await fetch(url, {
        method: 'GET',
        headers: {
          ...headers,
          'Accept': 'application/vnd.openxmlformats-officedocument.presentationml.presentation'
        }
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const arrayBuffer = await response.arrayBuffer();
      this.jszip = await new JSZip().loadAsync(arrayBuffer);
      
      // 加载主题
      await this.loadTheme();
      
      // 解析presentation.xml
      const presentationXml = await this.jszip.file("ppt/presentation.xml").async("text");
      const parser = new DOMParser();
      this.presentation = parser.parseFromString(presentationXml, "text/xml");
      
      // 获取幻灯片尺寸
      const sldSz = this.presentation.getElementsByTagNameNS("*", "sldSz")[0];
      if (sldSz) {
        this.slideWidth = parseInt(sldSz.getAttribute("cx")) / 9525;
        this.slideHeight = parseInt(sldSz.getAttribute("cy")) / 9525;
      }
      
      // 获取幻灯片数量
      const slideNodes = this.presentation.getElementsByTagNameNS("*", "sldId");
      this.totalSlides = slideNodes.length;
      
      // 预加载所有幻灯片
      await this.loadAllSlides();
      
      return true;
    } catch (error) {
      console.error('加载PPTX文件失败:', error);
      throw error;
    }
  }

  async loadTheme() {
    try {
      const themeFile = this.jszip.file("ppt/theme/theme1.xml");
      if (themeFile) {
        const themeXml = await themeFile.async("text");
        this.theme = new DOMParser().parseFromString(themeXml, "text/xml");
      }
    } catch (error) {
      console.warn('加载主题失败:', error);
    }
  }

  async loadAllSlides() {
    this.slides = [];
    for (let i = 1; i <= this.totalSlides; i++) {
      const slideContent = await this.loadSlide(i);
      this.slides.push(slideContent);
    }
  }

  async loadSlide(slideNumber) {
    const slideFile = this.jszip.file(`ppt/slides/slide${slideNumber}.xml`);
    if (!slideFile) {
      throw new Error(`找不到第${slideNumber}页`);
    }
    
    const slideXml = await slideFile.async("text");
    return slideXml;
  }

  async render(container) {
    this.container = container;
    await this.renderSlide(this.currentSlide);
  }

  async renderSlide(slideNumber) {
    if (!this.container || slideNumber < 1 || slideNumber > this.totalSlides) {
      return;
    }

    const slideXml = this.slides[slideNumber - 1];
    const parser = new DOMParser();
    const slideDoc = parser.parseFromString(slideXml, "text/xml");
    
    // 清空容器
    this.container.innerHTML = '';
    
    // 创建幻灯片容器
    const slideContainer = document.createElement('div');
    slideContainer.className = 'pptx-slide';
    slideContainer.style.width = `${this.slideWidth}px`;
    slideContainer.style.height = `${this.slideHeight}px`;
    slideContainer.style.transform = `scale(${this.scale})`;
    slideContainer.style.transformOrigin = 'center top';
    
    // 渲染背景
    await this.renderBackground(slideDoc, slideContainer);
    
    // 渲染形状和文本
    const spTree = slideDoc.getElementsByTagNameNS("*", "spTree")[0];
    if (spTree) {
      await this.renderShapeTree(spTree, slideContainer);
    }
    
    // 添加到容器
    this.container.appendChild(slideContainer);
    this.currentSlide = slideNumber;
  }

  async renderBackground(slideDoc, container) {
    const bg = slideDoc.getElementsByTagNameNS("*", "bg")[0];
    if (bg) {
      const bgPr = bg.getElementsByTagNameNS("*", "bgPr")[0];
      if (bgPr) {
        const solidFill = bgPr.getElementsByTagNameNS("*", "solidFill")[0];
        if (solidFill) {
          const srgbClr = solidFill.getElementsByTagNameNS("*", "srgbClr")[0];
          if (srgbClr) {
            const color = srgbClr.getAttribute("val");
            container.style.backgroundColor = `#${color}`;
          }
        }
      }
    }
  }

  async renderShapeTree(spTree, container) {
    const shapes = spTree.getElementsByTagNameNS("*", "sp");
    for (const shape of shapes) {
      const nvSpPr = shape.getElementsByTagNameNS("*", "nvSpPr")[0];
      const spPr = shape.getElementsByTagNameNS("*", "spPr")[0];
      const txBody = shape.getElementsByTagNameNS("*", "txBody")[0];
      
      // 创建形状容器
      const shapeElement = document.createElement('div');
      shapeElement.className = 'pptx-shape';
      
      // 设置位置和尺寸
      if (spPr) {
        const xfrm = spPr.getElementsByTagNameNS("*", "xfrm")[0];
        if (xfrm) {
          const off = xfrm.getElementsByTagNameNS("*", "off")[0];
          const ext = xfrm.getElementsByTagNameNS("*", "ext")[0];
          
          if (off && ext) {
            const x = parseInt(off.getAttribute("x")) / 9525;
            const y = parseInt(off.getAttribute("y")) / 9525;
            const width = parseInt(ext.getAttribute("cx")) / 9525;
            const height = parseInt(ext.getAttribute("cy")) / 9525;
            
            shapeElement.style.position = "absolute";
            shapeElement.style.left = `${x}px`;
            shapeElement.style.top = `${y}px`;
            shapeElement.style.width = `${width}px`;
            shapeElement.style.height = `${height}px`;
          }
        }
        
        // 设置形状样式
        const solidFill = spPr.getElementsByTagNameNS("*", "solidFill")[0];
        if (solidFill) {
          const srgbClr = solidFill.getElementsByTagNameNS("*", "srgbClr")[0];
          if (srgbClr) {
            shapeElement.style.backgroundColor = `#${srgbClr.getAttribute("val")}`;
          }
        }
      }
      
      // 渲染文本
      if (txBody) {
        const paragraphs = txBody.getElementsByTagNameNS("*", "p");
        for (const p of paragraphs) {
          const textRuns = p.getElementsByTagNameNS("*", "r");
          const paragraph = document.createElement('div');
          paragraph.className = 'pptx-paragraph';
          
          // 设置段落样式
          const pPr = p.getElementsByTagNameNS("*", "pPr")[0];
          if (pPr) {
            const algn = pPr.getElementsByTagNameNS("*", "algn")[0];
            if (algn) {
              paragraph.style.textAlign = algn.getAttribute("val") || 'left';
            }
            
            const indent = pPr.getElementsByTagNameNS("*", "indent")[0];
            if (indent) {
              paragraph.style.marginLeft = `${parseInt(indent.getAttribute("left")) / 9525}px`;
            }
          }
          
          for (const r of textRuns) {
            const textElement = document.createElement('span');
            textElement.className = 'pptx-text';
            
            // 设置文本内容
            const t = r.getElementsByTagNameNS("*", "t")[0];
            if (t) {
              textElement.textContent = t.textContent;
            }
            
            // 设置文本样式
            const rPr = r.getElementsByTagNameNS("*", "rPr")[0];
            if (rPr) {
              const sz = rPr.getAttribute("sz");
              if (sz) {
                textElement.style.fontSize = `${parseInt(sz) / 100}pt`;
              }
              
              const b = rPr.getAttribute("b") === "1";
              if (b) {
                textElement.style.fontWeight = "bold";
              }
              
              const i = rPr.getAttribute("i") === "1";
              if (i) {
                textElement.style.fontStyle = "italic";
              }
              
              const solidFill = rPr.getElementsByTagNameNS("*", "solidFill")[0];
              if (solidFill) {
                const srgbClr = solidFill.getElementsByTagNameNS("*", "srgbClr")[0];
                if (srgbClr) {
                  textElement.style.color = `#${srgbClr.getAttribute("val")}`;
                }
              }
              
              const latin = rPr.getElementsByTagNameNS("*", "latin")[0];
              if (latin) {
                textElement.style.fontFamily = latin.getAttribute("typeface");
              }
            }
            
            paragraph.appendChild(textElement);
          }
          
          shapeElement.appendChild(paragraph);
        }
      }
      
      container.appendChild(shapeElement);
    }
  }

  goToSlide(slideNumber) {
    this.renderSlide(slideNumber);
  }

  setZoom(scale) {
    this.scale = scale;
    const slide = this.container?.querySelector('.pptx-slide');
    if (slide) {
      slide.style.transform = `scale(${scale})`;
    }
  }

  getTotalSlides() {
    return this.totalSlides;
  }

  getCurrentSlide() {
    return this.currentSlide;
  }

  destroy() {
    if (this.container) {
      this.container.innerHTML = '';
    }
    this.container = null;
    this.slides = [];
    this.jszip = null;
    this.presentation = null;
    this.theme = null;
  }
}

// 导出为全局变量
window.PptxJS = PptxJS; 