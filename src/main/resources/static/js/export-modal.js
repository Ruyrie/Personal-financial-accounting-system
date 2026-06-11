(() => {
    // Excel 导出弹窗脚本：选择导出范围、月份和收入/支出类型后发起下载。
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
                    <p>选择要导出的 Excel 数据范围和月份。</p>
                </div>
                <button class="icon-button" type="button" aria-label="关闭" data-close-export-modal>×</button>
            </div>
            <div class="export-month-panel">
                <div class="export-month-head">
                    <div>
                        <strong>导出月份</strong>
                        <span data-export-range-summary>默认导出所选月份的数据</span>
                    </div>
                    <label class="export-all-months">
                        <input type="checkbox" data-export-all-months>
                        <span class="export-toggle-track" aria-hidden="true">
                            <span></span>
                        </span>
                        <span>全部月份</span>
                    </label>
                </div>
                <div class="export-month-field" data-export-month-picker>
                    <span>选择月份</span>
                    <input type="hidden" data-export-month>
                    <button class="glass-select export-month-trigger" type="button" data-export-month-trigger>
                        <span data-export-month-label>选择月份</span>
                        <span class="date-icon" aria-hidden="true"></span>
                    </button>
                </div>
                <div class="date-modal-backdrop export-month-modal-backdrop" data-export-month-modal hidden>
                    <div class="glass-menu month-menu export-month-dialog" role="dialog" aria-modal="true" aria-label="选择导出月份">
                        <div class="month-head">
                            <button type="button" data-export-year-prev>上一年</button>
                            <strong data-export-year-label>2026</strong>
                            <button type="button" data-export-year-next>下一年</button>
                        </div>
                        <div class="month-grid" data-export-month-grid></div>
                        <div class="month-actions">
                            <button type="button" data-export-year-current>返回本月</button>
                        </div>
                    </div>
                </div>
                <div class="export-alert" data-export-alert hidden>所选月份暂无可导出的数据，请重新选择。</div>
            </div>
            <div class="export-options" aria-label="导出类型">
                <button class="export-option income-option" type="button" data-export-type="1">
                    <span class="export-option-icon">+</span>
                    <strong>收入情况</strong>
                    <em data-export-copy="income">仅导出所选月份收入记录</em>
                </button>
                <button class="export-option expense-option" type="button" data-export-type="2">
                    <span class="export-option-icon">−</span>
                    <strong>支出情况</strong>
                    <em data-export-copy="expense">仅导出所选月份支出记录</em>
                </button>
                <button class="export-option all-option" type="button" data-export-type="">
                    <span class="export-option-icon">¥</span>
                    <strong>全部收支</strong>
                    <em data-export-copy="all">导出所选月份全部收支记录</em>
                </button>
            </div>
        </section>
    `;
    document.body.appendChild(modal);

    let activeBase = '/transactions/export';
    const monthInput = modal.querySelector('[data-export-month]');
    const monthTrigger = modal.querySelector('[data-export-month-trigger]');
    const monthModal = modal.querySelector('[data-export-month-modal]');
    const monthLabel = modal.querySelector('[data-export-month-label]');
    const monthGrid = modal.querySelector('[data-export-month-grid]');
    const yearLabel = modal.querySelector('[data-export-year-label]');
    const allMonthsInput = modal.querySelector('[data-export-all-months]');
    const allMonthsLabel = modal.querySelector('.export-all-months');
    const rangeSummary = modal.querySelector('[data-export-range-summary]');
    const alertBox = modal.querySelector('[data-export-alert]');
    const exportButtons = modal.querySelectorAll('[data-export-type]');
    const exportCopies = {
        income: modal.querySelector('[data-export-copy="income"]'),
        expense: modal.querySelector('[data-export-copy="expense"]'),
        all: modal.querySelector('[data-export-copy="all"]')
    };
    const copyByScope = {
        selected: {
            income: '仅导出所选月份收入记录',
            expense: '仅导出所选月份支出记录',
            all: '导出所选月份全部收支记录'
        },
        allMonths: {
            income: '导出全部月份收入记录',
            expense: '导出全部月份支出记录',
            all: '导出全部月份全部收支记录'
        }
    };

    // 获取当前年月，格式为 yyyy-MM。
    const currentMonth = () => {
        const now = new Date();
        return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
    };
    const now = new Date();
    let visibleYear = now.getFullYear();

    // 将 yyyy-MM 格式转换成页面展示用的“yyyy年MM月”。
    const formatMonth = (value) => {
        if (!value) {
            return '';
        }
        const [year, month] = value.split('-');
        return `${year}年${month}月`;
    };

    // 在导出弹窗中显示校验或接口错误提示。
    const showAlert = (message) => {
        alertBox.textContent = message;
        alertBox.hidden = false;
    };

    // 清除导出弹窗中的提示信息。
    const clearAlert = () => {
        alertBox.hidden = true;
    };

    // 关闭导出月份选择面板。
    const closeMonthMenu = () => {
        monthModal.hidden = true;
    };

    // 打开导出月份选择面板，并先渲染月份按钮。
    const openMonthMenu = () => {
        renderExportMonths();
        monthModal.hidden = false;
    };

    // 渲染当前可见年份的 12 个月份按钮，并同步选中态。
    const renderExportMonths = () => {
        yearLabel.textContent = visibleYear;
        monthLabel.textContent = monthInput.value ? formatMonth(monthInput.value) : '选择月份';
        monthGrid.innerHTML = '';
        Array.from({ length: 12 }, (_, index) => {
            const month = String(index + 1).padStart(2, '0');
            const value = `${visibleYear}-${month}`;
            const button = document.createElement('button');
            button.type = 'button';
            button.textContent = `${index + 1}月`;
            button.classList.toggle('is-selected', monthInput.value === value);
            button.addEventListener('click', () => {
                monthInput.value = value;
                renderExportMonths();
                closeMonthMenu();
                clearAlert();
            });
            monthGrid.appendChild(button);
        });
    };

    // 同步“全部月份”开关带来的表单可用状态和文案。
    const syncMonthMode = () => {
        const isAllMonths = allMonthsInput.checked;
        const scope = isAllMonths ? copyByScope.allMonths : copyByScope.selected;
        monthInput.disabled = isAllMonths;
        monthTrigger.disabled = isAllMonths;
        allMonthsLabel.classList.toggle('is-checked', isAllMonths);
        monthInput.closest('.export-month-field').classList.toggle('is-disabled', isAllMonths);
        rangeSummary.textContent = isAllMonths ? '将忽略月份选择，导出所有月份数据' : '默认导出所选月份的数据';
        exportCopies.income.textContent = scope.income;
        exportCopies.expense.textContent = scope.expense;
        exportCopies.all.textContent = scope.all;
        if (isAllMonths) {
            closeMonthMenu();
        }
        clearAlert();
    };

    // 控制导出按钮的加载态，防止重复点击。
    const setButtonsLoading = (isLoading) => {
        exportButtons.forEach((button) => {
            button.disabled = isLoading;
            button.classList.toggle('is-loading', isLoading);
        });
    };

    // 关闭导出弹窗并还原临时状态。
    const closeModal = () => {
        modal.hidden = true;
        document.body.classList.remove('modal-open');
        closeMonthMenu();
        clearAlert();
        setButtonsLoading(false);
    };

    // 打开导出弹窗，并从触发按钮携带的导出 URL 中恢复筛选条件。
    const openModal = (trigger) => {
        activeBase = trigger.dataset.exportBase || trigger.getAttribute('href') || '/transactions/export';
        const baseUrl = new URL(activeBase, window.location.origin);
        monthInput.value = baseUrl.searchParams.get('month') || currentMonth();
        visibleYear = Number(monthInput.value.slice(0, 4)) || now.getFullYear();
        allMonthsInput.checked = false;
        syncMonthMode();
        renderExportMonths();
        modal.hidden = false;
        document.body.classList.add('modal-open');
    };

    // 构造导出或预检查接口 URL，保留当前筛选条件。
    const buildUrl = (type, pathname) => {
        const url = new URL(activeBase, window.location.origin);
        if (type) {
            url.searchParams.set('type', type);
        } else {
            url.searchParams.delete('type');
        }
        if (allMonthsInput.checked) {
            url.searchParams.delete('month');
        } else {
            url.searchParams.set('month', monthInput.value);
        }
        if (pathname) {
            url.pathname = pathname;
        }
        return url;
    };

    // 构造最终 Excel 下载地址。
    const buildExportUrl = (type) => {
        const url = buildUrl(type);
        return `${url.pathname}${url.search}`;
    };

    // 调用列表 API 预检查当前条件下是否存在可导出的记录。
    const countExportRows = async (type) => {
        const url = buildUrl(type, '/api/transactions');
        url.searchParams.set('page', '1');
        url.searchParams.set('size', '1');
        const response = await fetch(`${url.pathname}${url.search}`, {
            headers: {
                Accept: 'application/json'
            }
        });
        if (!response.ok) {
            throw new Error('export preview failed');
        }
        const result = await response.json();
        return Number(result.total || 0);
    };

    triggers.forEach((trigger) => {
        trigger.addEventListener('click', (event) => {
            event.preventDefault();
            openModal(trigger);
        });
    });

    allMonthsInput.addEventListener('change', syncMonthMode);
    monthInput.addEventListener('input', clearAlert);

    exportButtons.forEach((button) => {
        button.addEventListener('click', async () => {
            clearAlert();
            if (!allMonthsInput.checked && !monthInput.value) {
                showAlert('请先选择要导出的月份，或勾选全部月份导出。');
                monthTrigger.focus();
                return;
            }
            setButtonsLoading(true);
            try {
                const total = await countExportRows(button.dataset.exportType);
                if (total === 0) {
                    showAlert(allMonthsInput.checked
                        ? '当前筛选条件下没有可导出的数据，请重新选择。'
                        : `${formatMonth(monthInput.value)}暂无可导出的数据，请重新选择月份，或勾选全部月份导出。`);
                    if (!allMonthsInput.checked) {
                        monthTrigger.focus();
                    }
                    return;
                }
            } catch (error) {
                showAlert('暂时无法确认导出数据，请稍后重试。');
                return;
            } finally {
                setButtonsLoading(false);
            }
            window.location.href = buildExportUrl(button.dataset.exportType);
        });
    });

    monthTrigger.addEventListener('click', (event) => {
        event.stopPropagation();
        if (monthTrigger.disabled) {
            return;
        }
        if (monthModal.hidden) {
            openMonthMenu();
        } else {
            closeMonthMenu();
        }
    });

    modal.querySelector('[data-export-year-prev]').addEventListener('click', () => {
        visibleYear -= 1;
        renderExportMonths();
    });

    modal.querySelector('[data-export-year-next]').addEventListener('click', () => {
        visibleYear += 1;
        renderExportMonths();
    });

    modal.querySelector('[data-export-year-current]').addEventListener('click', () => {
        monthInput.value = currentMonth();
        visibleYear = now.getFullYear();
        renderExportMonths();
        closeMonthMenu();
        clearAlert();
    });

    modal.querySelectorAll('[data-close-export-modal]').forEach((button) => {
        button.addEventListener('click', closeModal);
    });

    monthModal.addEventListener('click', (event) => {
        if (event.target === monthModal) {
            closeMonthMenu();
        }
    });

    modal.addEventListener('click', (event) => {
        if (event.target === modal) {
            closeModal();
        }
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && !modal.hidden) {
            closeMonthMenu();
            closeModal();
        }
    });
})();
