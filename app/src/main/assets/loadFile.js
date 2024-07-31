// load book content
fetch('%s')
    .then(res => res.blob())
    .then(b => {
        console.log('file downloaded');
        const input = document.querySelector('#file-input');
        const f = new File([b], '%s');
        const dataTransfer = new DataTransfer();
        dataTransfer.items.add(f);
        input.files = dataTransfer.files;

        input.dispatchEvent(new Event('change', { bubbles: true }));
    })
    .catch(err => console.error(err));

// responsive injection of scroll listener
let scrollSetTimeoutId;
let done;
new MutationObserver((_, observer) => {
    if (done) return;
    const node = document.querySelector('foliate-view');
    if (node && !scrollSetTimeoutId) {
        scrollSetTimeoutId = setTimeout(() => {
            Android.renderComplete();
            node.addEventListener('relocate', e => {
                Android.updateScrollState(e.detail.fraction);
            });
            console.log('reader scroll listener attached');

            // change layout to scrolling
//            const menuBtn = Array.from(document.getElementsByTagName('li'))
//                .filter(it => it.getAttribute('role') !== 'none' && it.getAttribute('aria-checked') === 'false')[0];
//            menuBtn.dispatchEvent(new Event('click'));
//            console.log('changed layout to scrolling')

            observer.disconnect();
            done = true;
            clearTimeout(scrollSetTimeoutId);
        }, %d);
    }
}).observe(document.querySelector('body'), {childList: true, subtree: true});