// %SOURCE_FILE_HEADER%
//

module hny2026_top (
    input wire sys_clk,
    input wire sys_rst_n,
    // 0 - R, 1 - B, 2 - G
    output wire [2:0] led
);

    reg [2:0] rst_sync;
    wire reset = ~rst_sync[0];
    always @(posedge sys_clk) rst_sync <= {sys_rst_n, rst_sync[2:1]};

    wire [2:0] led_inv;
    assign led = ~led_inv;

    HNY2026 hny2026 (
        .clock(sys_clk),
        .reset(reset),
        .io_ledR(led_inv[0]),
        .io_ledG(led_inv[2]),
        .io_ledB(led_inv[1])
    );

endmodule // hny2026_top
