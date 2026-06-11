(function () {
    // 修改密码弹窗脚本：动态创建弹窗，并绑定打开、关闭、Esc 退出行为。
    const triggers = document.querySelectorAll('[data-open-password-modal]');
    if (!triggers.length) {
        return;
    }

    const modal = document.createElement('div');
    modal.className = 'modal-backdrop password-modal-backdrop';
    modal.hidden = true;
    modal.innerHTML = `
        <section class="category-modal password-modal" role="dialog" aria-modal="true" aria-labelledby="password-modal-title">
            <div class="modal-head">
                <div>
                    <h2 id="password-modal-title">修改密码</h2>
                    <p>设置新的登录密码，不能与旧密码相同。</p>
                </div>
                <button class="icon-button" type="button" aria-label="关闭" data-close-password-modal>×</button>
            </div>
            <form class="modal-form" action="/profile/password" method="post">
                <label>
                    新密码
                    <input name="password" type="password" autocomplete="new-password" minlength="6" required>
                </label>
                <label>
                    确认新密码
                    <input name="confirmPassword" type="password" autocomplete="new-password" minlength="6" required>
                </label>
                <div class="form-actions">
                    <button class="secondary" type="button" data-close-password-modal>取消</button>
                    <button type="submit">保存密码</button>
                </div>
            </form>
        </section>
    `;
    document.body.appendChild(modal);

    const form = modal.querySelector('form');
    const firstInput = modal.querySelector('input');

    // 关闭弹窗时恢复页面滚动状态并清空表单。
    const closeModal = () => {
        modal.hidden = true;
        document.body.classList.remove('modal-open');
        form.reset();
    };

    // 打开弹窗后聚焦第一个密码输入框。
    const openModal = () => {
        modal.hidden = false;
        document.body.classList.add('modal-open');
        window.setTimeout(() => firstInput.focus(), 80);
    };

    triggers.forEach((trigger) => {
        trigger.addEventListener('click', openModal);
    });

    modal.querySelectorAll('[data-close-password-modal]').forEach((button) => {
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
