(() => {
    const triggers = document.querySelectorAll('[data-export-trigger]');
    if (!triggers.length) {
        return;
    }

    const modal = document.createElement('div');
    modal.className = 'modal-backdrop export-modal-backdrop';
    modal.dataset.exportModal = '';
    modal.hidden = true;
    modal.innerHTML = `
        <section class="export-modal" role="dialog" aria-modal="true" aria-labelledby="export-modal-title">
            <div class="modal-head">
                <div>
                    <h2 id="export-modal-title">选择导出类型</h2>
                    <p>选择要导出的 Excel 数据范围。</p>
                </div>
                <button class="icon-button" type="button" aria-label="关闭" data-close-export-modal>×</button>
            </div>
            <div class="export-options" aria-label="导出类型">
                <button class="export-option income-option" type="button" data-export-type="1">
                    <span class="export-option-icon">+</span>
                    <strong>收入情况</strong>
                    <em>仅导出收入记录</em>
                </button>
                <button class="export-option expense-option" type="button" data-export-type="2">
                    <span class="export-option-icon">−</span>
                    <strong>支出情况</strong>
                    <em>仅导出支出记录</em>
                </button>
                <button class="export-option all-option" type="button" data-export-type="">
                    <span class="export-option-icon">¥</span>
                    <strong>总体收支情况</strong>
                    <em>导出全部收支记录</em>
                </button>
            </div>
        </section>
    `;
    document.body.appendChild(modal);

    let activeBase = '/transactions/export';

    const closeModal = () => {
        modal.hidden = true;
        document.body.classList.remove('modal-open');
    };

    const openModal = (trigger) => {
        activeBase = trigger.dataset.exportBase || trigger.getAttribute('href') || '/transactions/export';
        modal.hidden = false;
        document.body.classList.add('modal-open');
    };

    const buildExportUrl = (type) => {
        const url = new URL(activeBase, window.location.origin);
        if (type) {
            url.searchParams.set('type', type);
        } else {
            url.searchParams.delete('type');
        }
        return `${url.pathname}${url.search}`;
    };

    triggers.forEach((trigger) => {
        trigger.addEventListener('click', (event) => {
            event.preventDefault();
            openModal(trigger);
        });
    });

    modal.querySelectorAll('[data-export-type]').forEach((button) => {
        button.addEventListener('click', () => {
            window.location.href = buildExportUrl(button.dataset.exportType);
        });
    });

    modal.querySelectorAll('[data-close-export-modal]').forEach((button) => {
        button.addEventListener('click', closeModal);
    });

    modal.addEventListener('click', (event) => {
        if (event.target === modal) {
            closeModal();
        }
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && !modal.hidden) {
            closeModal();
        }
    });
})();
