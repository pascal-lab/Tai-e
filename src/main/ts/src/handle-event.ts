export function eventHandler(elem: HTMLElement, fileHandler: (file: File)=>void){
    elem.addEventListener('dragenter', function(event) {
        event.preventDefault();
        elem.style.background = '#f7f7f7';
        elem.textContent = "松开文件进行处理";
    });
    elem.addEventListener('dragleave', function(event) {
        event.preventDefault();
        elem.style.background = '#ffffff';
        elem.textContent = "拖拽文件进入此处";
    });
    elem.addEventListener('dragover', function(event) {
        event.preventDefault();
    });
    elem.addEventListener('drop', function(event){
        event.preventDefault();
        elem.style.background = '#ffffff';

        let file = (event as DragEvent).dataTransfer?.files[0];

        if(file === undefined){
            elem.textContent = "未接收到文件";
            return;
        }
        if(!file.name.endsWith('.yml')){
            elem.textContent = "接收到非yaml文件";
            return;
        }
        elem.textContent = "拖拽文件进入此处";
        console.log('handle event over, begin building graph');
        fileHandler(file);
    });
}