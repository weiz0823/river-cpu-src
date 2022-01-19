# ID阶段设计

## ALU输入数据选择

- 端口1
  - rs1
  - pc
  - 0: lui
  - rs1域0扩展：csrr_i
- 端口2
  - rs2
  - imm
  - 4: jal/jalr