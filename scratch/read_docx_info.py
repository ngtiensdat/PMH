import os
import sys
import zipfile
import xml.etree.ElementTree as ET

# Reconfigure stdout to utf-8 to prevent console encoding errors on Windows
if sys.stdout:
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except AttributeError:
        pass

def extract_docx_text(docx_path, out_txt_path):
    print(f"Extracting: {repr(docx_path)} -> {repr(out_txt_path)}")
    if not os.path.exists(docx_path):
        print(f"File not found: {repr(docx_path)}")
        return
    
    try:
        with zipfile.ZipFile(docx_path) as doc:
            xml_content = doc.read('word/document.xml')
            root = ET.fromstring(xml_content)
            
            # Define namespaces for xpath/iter matching
            ns = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
            
            # Open output file
            with open(out_txt_path, 'w', encoding='utf-8') as f:
                paragraph_count = 0
                for p_node in root.iter(f"{{{ns['w']}}}p"):
                    # Find all text nodes inside this paragraph
                    texts = []
                    for t_node in p_node.iter(f"{{{ns['w']}}}t"):
                        if t_node.text:
                            texts.append(t_node.text)
                    if texts:
                        paragraph_text = "".join(texts)
                        f.write(paragraph_text + "\n")
                        paragraph_count += 1
                
                print(f"Done. Extracted {paragraph_count} paragraphs.")
    except Exception as e:
        print(f"Error extracting: {e}")

if __name__ == '__main__':
    doc1 = r"e:\PMH\project\docs\Flow\LUỒNG SCL_PTYC_GPDN_DNMB_EVNNPC_QTCTKH_251110_v4.8.docx"
    doc2 = r"e:\PMH\project\docs\Flow\v2.7_SCL TKCT Ứng dụng (1).docx"
    
    out1 = r"e:\PMH\scratch\flow_doc1_extracted.txt"
    out2 = r"e:\PMH\scratch\flow_doc2_extracted.txt"
    
    extract_docx_text(doc1, out1)
    extract_docx_text(doc2, out2)
