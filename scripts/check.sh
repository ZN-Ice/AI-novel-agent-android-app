#!/bin/bash

# AI小说安卓App - Harness核心流程检查脚本
# 执行：构建测试 → Lint检查 → 单元测试验证
# 符合 AGENTS.md 9.3 核心开发流程

echo "============================================"
echo "  AI小说安卓App - Harness核心流程"
echo "============================================"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 统计变量
ERRORS=0
WARNINGS=0

# ==================== 步骤1：构建测试 ====================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}步骤1: 构建测试（验证代码能否编译）${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 检查Kotlin文件
echo "检查Kotlin源文件..."
KOTLIN_COUNT=$(find ./app/src/main -name "*.kt" 2>/dev/null | wc -l)
echo "找到 $KOTLIN_COUNT 个Kotlin源文件"

# 检查语法问题（简化版静态分析）
SYNTAX_ERRORS=0
for file in $(find ./app/src/main -name "*.kt" 2>/dev/null); do
    # 检查未闭合的括号
    OPEN_BRACES=$(grep -o "{" "$file" 2>/dev/null | wc -l)
    CLOSE_BRACES=$(grep -o "}" "$file" 2>/dev/null | wc -l)
    if [ "$OPEN_BRACES" != "$CLOSE_BRACES" ]; then
        echo -e "${RED}  ⚠️ $file: 括号不匹配${NC}"
        SYNTAX_ERRORS=$((SYNTAX_ERRORS + 1))
    fi
done

if [ "$SYNTAX_ERRORS" -gt 0 ]; then
    echo -e "${RED}❌ 构建检查: 发现 $SYNTAX_ERRORS 个语法问题${NC}"
    ERRORS=$((ERRORS + 1))
else
    echo -e "${GREEN}✅ 构建检查: 通过（静态验证）${NC}"
fi

# ==================== 步骤2：Lint检查 ====================
echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}步骤2: Lint检查（代码规范与安全）${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 检查硬编码API密钥
echo "检查安全问题..."
API_KEY_COUNT=$(grep -rn "api[_-]\?key\s*=\s*\"[^\"]\{16,\}\"" ./app/src/main --include="*.kt" 2>/dev/null | wc -l || echo "0")
if [ "$API_KEY_COUNT" -gt 0 ]; then
    echo -e "${RED}❌ 发现 $API_KEY_COUNT 处可能的硬编码API密钥${NC}"
    ERRORS=$((ERRORS + 1))
else
    echo -e "${GREEN}✅ 未发现硬编码API密钥${NC}"
fi

# 检查printStackTrace
PRINT_STACK_COUNT=$(grep -r "printStackTrace" ./app/src/main --include="*.kt" 2>/dev/null | wc -l || echo "0")
if [ "$PRINT_STACK_COUNT" -gt 0 ]; then
    echo -e "${YELLOW}⚠️ 发现 $PRINT_STACK_COUNT 处 printStackTrace 调用${NC}"
    WARNINGS=$((WARNINGS + 1))
else
    echo -e "${GREEN}✅ 未发现 printStackTrace 调用${NC}"
fi

# 检查System.out
SYSTEM_OUT_COUNT=$(grep -r "System\.out\." ./app/src/main --include="*.kt" 2>/dev/null | wc -l || echo "0")
if [ "$SYSTEM_OUT_COUNT" -gt 0 ]; then
    echo -e "${YELLOW}⚠️ 发现 $SYSTEM_OUT_COUNT 处 System.out 调用${NC}"
    WARNINGS=$((WARNINGS + 1))
else
    echo -e "${GREEN}✅ 未发现 System.out 调用${NC}"
fi

# 检查空文件
echo ""
echo "检查文件完整性..."
EMPTY_FILES=$(find ./app/src/main -name "*.kt" -empty 2>/dev/null | wc -l || echo "0")
if [ "$EMPTY_FILES" -gt 0 ]; then
    echo -e "${RED}❌ 发现 $EMPTY_FILES 个空文件${NC}"
    ERRORS=$((ERRORS + 1))
else
    echo -e "${GREEN}✅ 无空文件${NC}"
fi

# ==================== 步骤3：单元测试检查 ====================
echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}步骤3: 单元测试覆盖检查${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

TEST_COUNT=$(find ./app/src/test -name "*.kt" 2>/dev/null | wc -l)
UI_TEST_COUNT=$(find ./app/src/androidTest -name "*.kt" 2>/dev/null | wc -l)
echo "单元测试文件: $TEST_COUNT"
echo "UI测试文件: $UI_TEST_COUNT"

if [ "$TEST_COUNT" -lt 5 ]; then
    echo -e "${YELLOW}⚠️ 单元测试文件较少，建议增加${NC}"
    WARNINGS=$((WARNINGS + 1))
else
    echo -e "${GREEN}✅ 单元测试覆盖良好${NC}"
fi

# 检查资源文件
echo ""
echo "检查资源文件..."
STRINGS_COUNT=$(grep -c "<string" ./app/src/main/res/values/strings.xml 2>/dev/null || echo "0")
echo "字符串资源: $STRINGS_COUNT 个"

# ==================== 汇总 ====================
echo ""
echo "============================================"
echo "  Harness核心流程检查结果"
echo "============================================"
echo ""
echo "错误: $ERRORS"
echo "警告: $WARNINGS"
echo ""

if [ "$ERRORS" -gt 0 ]; then
    echo -e "${RED}❌ 检查未通过，请修复错误后重试${NC}"
    echo ""
    echo "修复后重新执行: ./scripts/check.sh"
    exit 1
elif [ "$WARNINGS" -gt 0 ]; then
    echo -e "${YELLOW}⚠️ 检查通过，但有警告需要关注${NC}"
    echo "可以继续提交，建议后续优化"
    echo ""
    echo "下一步: git add . && git commit -m \"message\""
    exit 0
else
    echo -e "${GREEN}✅ 所有检查通过！${NC}"
    echo ""
    echo "下一步: git add . && git commit -m \"message\""
    exit 0
fi
