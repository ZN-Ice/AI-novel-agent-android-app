#!/bin/bash

# AI小说安卓App - 代码检查脚本
# 当Gradle不可用时执行静态检查

echo "============================================"
echo "  AI小说安卓App - 代码检查"
echo "============================================"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 统计变量
ERRORS=0
WARNINGS=0

# 检查Kotlin文件
echo "=== 检查Kotlin源文件 ==="
KOTLIN_COUNT=$(find ./app/src/main -name "*.kt" 2>/dev/null | wc -l)
echo "找到 $KOTLIN_COUNT 个Kotlin源文件"

# 检查硬编码API密钥
echo ""
echo "=== 检查安全问题 ==="
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
echo "=== 检查文件完整性 ==="
EMPTY_FILES=$(find ./app/src/main -name "*.kt" -empty 2>/dev/null | wc -l || echo "0")
if [ "$EMPTY_FILES" -gt 0 ]; then
    echo -e "${RED}❌ 发现 $EMPTY_FILES 个空文件${NC}"
    ERRORS=$((ERRORS + 1))
else
    echo -e "${GREEN}✅ 无空文件${NC}"
fi

# 检查测试文件
echo ""
echo "=== 检查测试覆盖 ==="
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
echo "=== 检查资源文件 ==="
STRINGS_COUNT=$(grep -c "<string" ./app/src/main/res/values/strings.xml 2>/dev/null || echo "0")
echo "字符串资源: $STRINGS_COUNT 个"

# 汇总
echo ""
echo "============================================"
echo "  检查结果汇总"
echo "============================================"
echo ""
echo "错误: $ERRORS"
echo "警告: $WARNINGS"
echo ""

if [ "$ERRORS" -gt 0 ]; then
    echo -e "${RED}❌ 检查未通过，请修复错误后重试${NC}"
    exit 1
elif [ "$WARNINGS" -gt 0 ]; then
    echo -e "${YELLOW}⚠️ 检查通过，但有警告需要关注${NC}"
    echo "可以继续提交，建议后续优化"
    exit 0
else
    echo -e "${GREEN}✅ 所有检查通过！${NC}"
    exit 0
fi
