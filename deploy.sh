#!/bin/bash
# =====================================================
# 智慧烟感系统 - K8s 一键部署脚本
# =====================================================

set -e

echo "🚀 开始部署智慧烟感系统..."

# 1. 创建命名空间
echo "📦 创建命名空间..."
kubectl apply -f k8s-deployment.yaml

# 2. 等待MySQL就绪
echo "⏳ 等待MySQL启动..."
kubectl wait --for=condition=ready pod -l app=mysql -n smoke-sensor --timeout=120s

# 3. 等待后端就绪
echo "⏳ 等待后端启动..."
kubectl wait --for=condition=ready pod -l app=backend -n smoke-sensor --timeout=120s

# 4. 等待前端就绪
echo "⏳ 等待前端启动..."
kubectl wait --for=condition=ready pod -l app=frontend -n smoke-sensor --timeout=60s

# 5. 获取访问地址
echo ""
echo "✅ 部署完成！"
echo ""
echo "📊 查看服务状态："
kubectl get pods -n smoke-sensor
echo ""
echo "🌐 访问地址："
NODE_IP=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[0].address}')
echo "   前端: http://${NODE_IP}:30080"
echo "   后端API: http://${NODE_IP}:30080/api/test"
echo ""
echo "📝 查看日志："
echo "   kubectl logs -f deployment/backend -n smoke-sensor"
echo "   kubectl logs -f deployment/frontend -n smoke-sensor"
echo ""
echo "🗑️  卸载："
echo "   kubectl delete namespace smoke-sensor"
