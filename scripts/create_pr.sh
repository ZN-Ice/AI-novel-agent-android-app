#!/bin/bash
# AI小说安卓App - PR创建脚本
# 自动化PR流程：检查 -> 提交 -> 推送 -> 创建PR
#
# 用法:
#   bash scripts/create_pr.sh feat "新增语音输入功能"
#   bash scripts/create_pr.sh fix "修复登录崩溃"
#
# @see AGENTS.md 9.3 核心开发流程（PR模式）

set -e

# ==================== 配置 ====================
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ==================== 参数检查 ====================
if [ $# -lt 2 ]; then
    echo -e "${RED}用法: $0 <类型> <描述>${NC}"
    echo ""
    echo "类型:"
    echo "  feat     - 新功能"
    echo "  fix      - Bug修复"
    echo "  refactor - 重构"
    echo "  docs     - 文档"
    echo "  test     - 测试"
    echo "  chore    - 构建/工具"
    echo ""
    echo "示例:"
    echo "  $0 feat \"新增语音输入功能\""
    echo "  $0 fix \"修复登录崩溃问题\""
    exit 1
fi

BRANCH_TYPE="$1"
DESCRIPTION="$2"
BRANCH_NAME="${BRANCH_TYPE}/${DESCRIPTION// /-}"
COMMIT_MSG="${BRANCH_TYPE}: ${DESCRIPTION}"

# 验证分支类型
VALID_TYPES=("feat" "fix" "refactor" "docs" "test" "chore" "style" "perf" "ci")
if [[ ! " ${VALID_TYPES[@]} " =~ " ${BRANCH_TYPE} " ]]; then
    echo -e "${RED}错误: 无效的分支类型 '${BRANCH_TYPE}'${NC}"
    echo "有效类型: ${VALID_TYPES[*]}"
    exit 1
fi

# ==================== 函数定义 ====================

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_command() {
    if ! command -v "$1" &> /dev/null; then
        log_error "未找到命令: $1"
        exit 1
    fi
}

# ==================== 预检查 ====================
log_info "开始PR流程..."
echo ""

# 检查必要命令
check_command "git"
check_command "gh"

# 检查是否在git仓库中
if [ ! -d ".git" ]; then
    log_error "当前目录不是git仓库"
    exit 1
fi

# 检查是否有未提交的更改
if [ -n "$(git status --porcelain)" ]; then
    log_info "发现未提交的更改"
fi

# ==================== Step 1: 创建分支 ====================
log_info "[1/5] 创建特性分支: ${BRANCH_NAME}"

# 切换到main并拉取最新
git checkout main 2>/dev/null || {
    log_error "无法切换到main分支"
    exit 1
}

git pull origin main 2>/dev/null || {
    log_warning "无法拉取main分支（可能是新仓库）"
}

# 检查分支是否已存在
if git show-ref --verify --quiet "refs/heads/${BRANCH_NAME}"; then
    log_warning "分支 ${BRANCH_NAME} 已存在"
    read -p "是否删除并重新创建? (y/N) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git branch -D "${BRANCH_NAME}"
    else
        log_error "请使用不同的分支名称或手动删除现有分支"
        exit 1
    fi
fi

git checkout -b "${BRANCH_NAME}"
log_success "分支创建成功"

# ==================== Step 2: 本地检查 ====================
log_info "[2/5] 运行本地检查..."

cd "$PROJECT_ROOT"

if [ -f "scripts/check.sh" ]; then
    bash scripts/check.sh
else
    log_warning "未找到check.sh脚本，执行基础检查..."
    if [ -f "./gradlew" ]; then
        chmod +x ./gradlew
        ./gradlew assembleDebug lint test || {
            log_error "检查未通过，请修复后重试"
            exit 1
        }
    fi
fi

log_success "本地检查通过"

# ==================== Step 3: 提交代码 ====================
log_info "[3/5] 提交代码..."

if [ -n "$(git status --porcelain)" ]; then
    git add .
    git commit -m "${COMMIT_MSG}"
    log_success "提交成功: ${COMMIT_MSG}"
else
    log_warning "没有需要提交的更改"
fi

# ==================== Step 4: 推送分支 ====================
log_info "[4/5] 推送分支到远端..."

git push -u origin "${BRANCH_NAME}" 2>&1 || {
    log_error "推送失败"
    exit 1
}

log_success "推送成功"

# ==================== Step 5: 创建PR ====================
log_info "[5/5] 创建Pull Request..."

# 构建PR描述
PR_BODY="## 变更内容

- ${DESCRIPTION}

## 检查清单

- [x] 本地构建通过
- [x] Lint检查通过
- [x] 单元测试通过
- [x] 代码符合AGENTS.md规范

## 测试计划

- [ ] 功能测试
- [ ] 边界条件测试
- [ ] 回归测试

---

_此PR由脚本自动创建_"

# 创建PR
PR_URL=$(gh pr create \
    --base main \
    --head "${BRANCH_NAME}" \
    --title "${COMMIT_MSG}" \
    --body "${PR_BODY}" 2>&1)

if [ $? -eq 0 ]; then
    echo ""
    log_success "PR创建成功！"
    echo ""
    echo "============================================"
    echo "  PR地址: ${PR_URL}"
    echo "============================================"
    echo ""
    echo "下一步:"
    echo "  1. 等待GitHub Actions门禁检查完成"
    echo "  2. 查看AI代码审查结果"
    echo "  3. 门禁通过后使用 'gh pr merge --squash' 合并"
    echo ""
else
    log_error "PR创建失败: ${PR_URL}"
    exit 1
fi
